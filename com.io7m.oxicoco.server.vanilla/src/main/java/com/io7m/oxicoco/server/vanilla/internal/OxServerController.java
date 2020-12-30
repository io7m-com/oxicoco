/*
 * Copyright © 2020 Mark Raynsford <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.oxicoco.server.vanilla.internal;

import com.io7m.oxicoco.errors.OxIRCErrorChannelNonexistent;
import com.io7m.oxicoco.errors.OxIRCErrorNickNonexistent;
import com.io7m.oxicoco.messages.OxIRCMessage;
import com.io7m.oxicoco.messages.OxIRCMessageParserFactoryType;
import com.io7m.oxicoco.names.OxChannelName;
import com.io7m.oxicoco.names.OxNickName;
import com.io7m.oxicoco.names.OxTopic;
import com.io7m.oxicoco.server.api.OxServerConfiguration;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import net.jcip.annotations.GuardedBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class OxServerController implements OxServerControllerType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(OxServerController.class);

  private final Subject<OxServerEventType> eventSubject;
  private final Observable<OxServerEventType> events;
  private final OxServerConfiguration configuration;
  private final Clock clock;
  private final OxIRCMessageParserFactoryType parsers;
  private final Supplier<OxServerClientID> idSupplier;
  private final OffsetDateTime timeStart;

  private final Object stateLock;
  @GuardedBy("stateLock")
  private final OxClientMap clientMap;
  @GuardedBy("stateLock")
  private final OxChannelMap channelMap;

  public OxServerController(
    final OxServerConfiguration inConfiguration,
    final Clock inClock,
    final OxIRCMessageParserFactoryType inParsers,
    final Supplier<OxServerClientID> inIdSupplier)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "inConfiguration");
    this.clock =
      Objects.requireNonNull(inClock, "inClock");
    this.parsers =
      Objects.requireNonNull(inParsers, "inParsers");
    this.idSupplier =
      Objects.requireNonNull(inIdSupplier, "idSupplier");
    this.eventSubject =
      PublishSubject.<OxServerEventType>create()
        .toSerialized();

    this.events = this.eventSubject;
    this.timeStart = OffsetDateTime.now(this.clock);
    this.stateLock = new Object();
    this.clientMap = new OxClientMap(this.idSupplier);
    this.channelMap = new OxChannelMap();
  }

  private static Stream<OxNickName> nickOrNothing(
    final OxServerClient client)
  {
    try {
      return Stream.of(client.nick());
    } catch (final OxNameNotRegisteredException e) {
      return Stream.empty();
    }
  }

  @Override
  public Observable<OxServerEventType> events()
  {
    return this.events;
  }

  @Override
  public OxServerClient clientCreate(
    final Socket socket)
  {
    Objects.requireNonNull(socket, "socket");

    final OxServerClient client;
    synchronized (this.stateLock) {
      client = this.clientMap.clientCreate(clientId -> {
        return this.clientCreateInternal(socket, clientId);
      });
    }

    this.eventSubject.onNext(OxServerClientCreated.of(client));
    return client;
  }

  private OxServerClient clientCreateInternal(
    final Socket socket,
    final OxServerClientID clientId)
  {
    return new OxServerClient(
      this.configuration,
      this.parsers,
      this,
      clientId,
      socket
    );
  }

  @Override
  public Duration uptime()
  {
    return Duration.between(this.timeStart, OffsetDateTime.now(this.clock));
  }

  @Override
  public Optional<OxNickName> clientSetNick(
    final OxServerClient client,
    final OxNickName name)
    throws OxClientException
  {
    Objects.requireNonNull(client, "client");
    Objects.requireNonNull(name, "name");

    final Optional<OxNickName> oldNameOpt;
    synchronized (this.stateLock) {
      oldNameOpt = this.clientMap.clientSetNick(client, name);
    }

    this.eventSubject.onNext(
      OxServerClientNickChanged.builder()
        .setClient(client)
        .setOldName(oldNameOpt)
        .setNewName(name)
        .build()
    );

    if (oldNameOpt.isPresent()) {
      try {
        final var oldUserId =
          client.userId().withNick(oldNameOpt.get());
        final var clientsWatching =
          this.clientsWatching(client);

        this.sendMessageToClients(
          OxIRCMessage.builder()
            .setRawText("")
            .setPrefix(":" + oldUserId.format())
            .setCommand("NICK")
            .setTrailing(":" + name.value())
            .build(),
          clientsWatching
        );
      } catch (final OxNameNotRegisteredException e) {
        // No problem; the client may be destroyed before a name is registered
      }
    }
    return oldNameOpt;
  }

  private Collection<OxServerClientID> clientsWatching(
    final OxServerClient client)
  {
    synchronized (this.stateLock) {
      final var clientsInChannels =
        this.channelMap.channelsFor(client).stream()
          .flatMap(name -> this.channelMap.channelClients(name).stream());

      return Stream.concat(Stream.of(client.id()), clientsInChannels)
        .collect(Collectors.toSet());
    }
  }

  @Override
  public void clientDestroy(
    final OxServerClient client)
  {
    Objects.requireNonNull(client, "client");

    try {
      final var clientsWatching =
        this.clientsWatching(client);

      this.sendMessageToClients(
        OxIRCMessage.builder()
          .setRawText("")
          .setPrefix(":" + client.userId().format())
          .setCommand("QUIT")
          .setTrailing("")
          .build(),
        clientsWatching
      );
    } catch (final OxNameNotRegisteredException e) {
      // No problem; the client may be destroyed before a name is registered
    }

    final var clientId = client.id();
    synchronized (this.stateLock) {
      this.clientMap.clientDestroy(client);
    }

    try {
      client.close();
    } catch (final IOException e) {
      LOG.error("error destroying client: ", e);
    }

    this.eventSubject.onNext(OxServerClientDestroyed.of(clientId));
  }

  @Override
  public OxNickName clientNick(
    final OxServerClient client)
    throws OxNameNotRegisteredException
  {
    Objects.requireNonNull(client, "client");

    synchronized (this.stateLock) {
      return this.clientMap.clientNick(client)
        .orElseThrow(() -> new OxNameNotRegisteredException(
          "Client has not registered a nick yet"));
    }
  }

  @Override
  public OxUserID clientUserId(
    final OxServerClient client)
    throws OxNameNotRegisteredException
  {
    Objects.requireNonNull(client, "client");

    synchronized (this.stateLock) {
      return this.clientMap.clientUserId(client)
        .orElseThrow(() -> new OxNameNotRegisteredException(
          "Client has not registered an ID yet"));
    }
  }

  @Override
  public OxChannelJoinResult channelJoin(
    final OxServerClient client,
    final OxChannelName channelName)
  {
    Objects.requireNonNull(client, "client");
    Objects.requireNonNull(channelName, "channelName");

    final OxChannelJoinResult result;
    synchronized (this.stateLock) {
      result = this.channelMap.channelJoin(
        client,
        channelName,
        name -> new OxChannel(this, name)
      );
    }

    switch (result.status()) {
      case CHANNEL_ALREADY_JOINED: {
        break;
      }
      case CHANNEL_JOINED_EXISTING: {
        this.eventSubject.onNext(
          OxServerChannelJoined.builder()
            .setChannel(result.channel())
            .setClient(client)
            .build()
        );
        break;
      }
      case CHANNEL_JOINED_CREATED: {
        this.eventSubject.onNext(
          OxServerChannelCreated.builder()
            .setChannel(result.channel())
            .build()
        );
        this.eventSubject.onNext(
          OxServerChannelJoined.builder()
            .setChannel(result.channel())
            .setClient(client)
            .build()
        );
        break;
      }
    }

    try {
      this.sendMessageToClients(
        OxIRCMessage.builder()
          .setRawText("")
          .setPrefix(":" + client.userId().format())
          .setCommand("JOIN")
          .setTrailing(":" + channelName.value())
          .build(),
        result.notifyUsers()
      );
    } catch (final OxNameNotRegisteredException e) {
      throw new IllegalStateException(e);
    }
    return result;
  }

  private void sendMessageToClients(
    final OxIRCMessage message,
    final Collection<OxServerClientID> targets)
  {
    final Collection<OxServerClient> notifyClients;
    synchronized (this.stateLock) {
      notifyClients =
        targets.stream()
          .flatMap(id -> this.clientMap.clientOf(id).stream())
          .collect(Collectors.toList());
    }

    notifyClients.forEach(notifyClient -> notifyClient.enqueueMessage(message));
  }

  @Override
  public List<OxNickName> channelNicks(
    final OxChannelName channelName)
  {
    Objects.requireNonNull(channelName, "channelName");

    synchronized (this.stateLock) {
      return this.channelMap.channelClients(channelName)
        .stream()
        .flatMap(id -> this.clientMap.clientOf(id).stream())
        .flatMap(OxServerController::nickOrNothing)
        .collect(Collectors.toList());
    }
  }

  @Override
  public OxChannelPartResult channelPart(
    final OxServerClient client,
    final OxChannelName channelName)
    throws OxClientException
  {
    Objects.requireNonNull(client, "client");
    Objects.requireNonNull(channelName, "channelName");

    final OxChannelPartResult result;
    synchronized (this.stateLock) {
      result = this.channelMap.channelPart(client, channelName);
    }

    this.eventSubject.onNext(
      OxServerChannelParted.builder()
        .setChannel(result.channel())
        .setClient(client)
        .build()
    );

    try {
      this.sendMessageToClients(
        OxIRCMessage.builder()
          .setRawText("")
          .setPrefix(":" + client.userId().format())
          .setCommand("PART")
          .setTrailing(":" + channelName.value())
          .build(),
        result.notifyUsers()
      );
    } catch (final OxNameNotRegisteredException e) {
      throw new IllegalStateException(e);
    }
    return result;
  }

  @Override
  public OxTopic channelGetTopic(
    final OxServerClient client,
    final OxChannelName channelName)
  {
    Objects.requireNonNull(client, "client");
    Objects.requireNonNull(channelName, "channelName");

    synchronized (this.stateLock) {
      return this.channelMap.channelTopic(channelName);
    }
  }

  @Override
  public void channelSetTopic(
    final OxServerClient client,
    final OxChannelName channelName,
    final OxTopic newTopic)
    throws OxClientException
  {
    Objects.requireNonNull(client, "client");
    Objects.requireNonNull(channelName, "channelName");
    Objects.requireNonNull(newTopic, "newTopic");

    synchronized (this.stateLock) {
      this.channelMap.channelTopicSet(channelName, newTopic);
    }

    final var channelClients =
      this.clientsWatchingChannel(client, channelName);

    try {
      this.sendMessageToClients(
        OxIRCMessage.builder()
          .setRawText("")
          .setPrefix(":" + client.userId().format())
          .setCommand("TOPIC")
          .addParameters(channelName.value())
          .setTrailing(":" + newTopic.value())
          .build(),
        channelClients
      );
    } catch (final OxNameNotRegisteredException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public int clientCount()
  {
    synchronized (this.stateLock) {
      return this.clientMap.clientCount();
    }
  }

  @Override
  public int channelCount()
  {
    synchronized (this.stateLock) {
      return this.channelMap.channelCount();
    }
  }

  @Override
  public void channelMessage(
    final OxServerClient client,
    final OxChannelName channelName,
    final String message)
    throws OxClientException
  {
    Objects.requireNonNull(client, "client");
    Objects.requireNonNull(channelName, "channelName");
    Objects.requireNonNull(message, "message");

    synchronized (this.stateLock) {
      if (this.channelMap.channelOf(channelName).isEmpty()) {
        throw new OxClientException(
          OxIRCErrorChannelNonexistent.builder().build());
      }
    }

    final var channelUsers =
      this.clientsWatchingChannel(client, channelName);

    try {
      this.sendMessageToClients(
        OxIRCMessage.builder()
          .setRawText("")
          .setPrefix(":" + client.userId().format())
          .setCommand("PRIVMSG")
          .addParameters(channelName.value())
          .setTrailing(":" + message)
          .build(),
        channelUsers
      );
    } catch (final OxNameNotRegisteredException e) {
      throw new IllegalStateException(e);
    }
  }

  private List<OxServerClientID> clientsWatchingChannel(
    final OxServerClient client,
    final OxChannelName channelName)
  {
    synchronized (this.stateLock) {
      return this.channelMap.channelClients(channelName)
        .stream()
        .filter(id -> !Objects.equals(id, client.id()))
        .collect(Collectors.toList());
    }
  }

  @Override
  public void clientMessage(
    final OxServerClient client,
    final OxNickName nickName,
    final String message)
    throws OxClientException
  {
    Objects.requireNonNull(client, "client");
    Objects.requireNonNull(nickName, "nickName");
    Objects.requireNonNull(message, "message");

    final Optional<OxServerClient> targetUser;
    synchronized (this.stateLock) {
      targetUser = this.clientMap.clientForNick(nickName);
    }

    if (targetUser.isPresent()) {
      try {
        final var target = targetUser.get();
        target.enqueueMessage(
          OxIRCMessage.builder()
            .setRawText("")
            .setPrefix(":" + client.userId().format())
            .setCommand("PRIVMSG")
            .setTrailing(":" + message)
            .build()
        );
        return;
      } catch (final OxNameNotRegisteredException e) {
        throw new IllegalStateException(e);
      }
    }

    throw new OxClientException(OxIRCErrorNickNonexistent.builder().build());
  }

  @Override
  public void close()
    throws IOException
  {
    final var exceptions = new OxExceptionTracker<IOException>();

    final Collection<OxServerClient> clientCollection;
    synchronized (this.stateLock) {
      clientCollection = this.clientMap.clients();
    }

    for (final var client : clientCollection) {
      try {
        client.close();
      } catch (final IOException e) {
        exceptions.addException(e);
      }
    }

    synchronized (this.stateLock) {
      this.clientMap.clear();
    }

    exceptions.throwIfNecessary();
  }
}
