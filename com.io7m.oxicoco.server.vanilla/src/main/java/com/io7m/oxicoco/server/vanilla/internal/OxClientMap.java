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

import com.io7m.oxicoco.errors.OxIRCErrorNickCollision;
import com.io7m.oxicoco.names.OxNickName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public final class OxClientMap
{
  private static final Logger LOG =
    LoggerFactory.getLogger(OxClientMap.class);

  private final HashMap<OxNickName, OxServerClientID> nickToId;
  private final HashMap<OxServerClientID, OxNickName> idToNick;
  private final HashMap<OxServerClientID, OxServerClient> clients;
  private final Supplier<OxServerClientID> idSupplier;

  public OxClientMap(
    final Supplier<OxServerClientID> inIdSupplier)
  {
    this.idSupplier =
      Objects.requireNonNull(inIdSupplier, "idSupplier");

    this.clients = new HashMap<>();
    this.nickToId = new HashMap<>();
    this.idToNick = new HashMap<>();
  }

  private OxServerClientID freshClientID()
  {
    while (true) {
      final var id = this.idSupplier.get();
      if (this.clients.containsKey(id)) {
        continue;
      }
      return id;
    }
  }

  public Collection<OxServerClient> clients()
  {
    return List.copyOf(this.clients.values());
  }

  public OxServerClient clientCreate(
    final Function<OxServerClientID, OxServerClient> creator)
  {
    Objects.requireNonNull(creator, "creator");

    final var clientId = this.freshClientID();
    final OxServerClient client = creator.apply(clientId);
    this.clients.put(clientId, client);
    return client;
  }

  public Optional<OxServerClient> clientOf(
    final OxServerClientID id)
  {
    Objects.requireNonNull(id, "id");
    return Optional.ofNullable(this.clients.get(id));
  }

  public Optional<OxServerClient> clientForNick(
    final OxNickName nickName)
  {
    return Optional.ofNullable(this.nickToId.get(nickName))
      .flatMap(id -> Optional.ofNullable(this.clients.get(id)));
  }

  public Optional<OxNickName> clientSetNick(
    final OxServerClient client,
    final OxNickName name)
    throws OxClientException
  {
    Objects.requireNonNull(client, "client");
    Objects.requireNonNull(name, "name");

    /*
     * Does this client have any nick at all?
     */

    final var clientId = client.id();
    final var currentNick = this.idToNick.get(clientId);
    if (currentNick == null) {

      /*
       * Does anyone else have the requested nick?
       */

      final var otherClient = this.nickToId.get(name);
      if (otherClient != null) {
        throw new OxClientException(
          OxIRCErrorNickCollision.builder()
            .setRequested(name)
            .setCurrent(name)
            .build()
        );
      }

      this.nickToId.put(name, clientId);
      this.idToNick.put(clientId, name);
      return Optional.empty();
    }

    /*
     * Does anyone else have the requested nick?
     */

    final var otherClient = this.nickToId.get(name);
    if (otherClient != null) {
      throw new OxClientException(
        OxIRCErrorNickCollision.builder()
          .setRequested(name)
          .setCurrent(name)
          .build()
      );
    }

    this.nickToId.remove(currentNick);
    this.nickToId.put(name, clientId);
    this.idToNick.put(clientId, name);
    return Optional.of(currentNick);
  }

  public void clientDestroy(
    final OxServerClient client)
  {
    Objects.requireNonNull(client, "client");

    final var clientId = client.id();

    try {
      this.nickToId.remove(client.nick());
    } catch (final OxNameNotRegisteredException e) {
      // Client might not be registered yet
    }

    this.idToNick.remove(clientId);
    this.clients.remove(clientId);
  }

  public Optional<OxNickName> clientNick(
    final OxServerClient client)
  {
    Objects.requireNonNull(client, "client");
    return Optional.ofNullable(this.idToNick.get(client.id()));
  }

  public Optional<OxUserID> clientUserId(
    final OxServerClient client)
  {
    Objects.requireNonNull(client, "client");

    final var nick =
      Optional.ofNullable(this.idToNick.get(client.id()));

    return nick.map(
      nickName ->
        OxUserID.builder()
          .setHost(client.host())
          .setUser(client.user())
          .setNick(nickName)
          .build()
    );
  }

  public void clear()
  {
    this.clients.clear();
    this.idToNick.clear();
    this.nickToId.clear();
  }

  public int clientCount()
  {
    return this.clients.size();
  }

}
