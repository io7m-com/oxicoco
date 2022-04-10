/*
 * Copyright © 2020 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

import com.io7m.oxicoco.names.OxChannelName;
import com.io7m.oxicoco.names.OxNickName;
import com.io7m.oxicoco.names.OxTopic;
import io.reactivex.rxjava3.core.Observable;

import java.io.Closeable;
import java.net.Socket;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * The internal server controller.
 */

public interface OxServerControllerType extends Closeable
{
  /**
   * @return An observable stream of server events
   */

  Observable<OxServerEventType> events();

  /**
   * Create a new client for the given socket.
   *
   * @param socket The socket
   *
   * @return A new client
   */

  OxServerClient clientCreate(Socket socket);

  /**
   * @return The server uptime
   */

  Duration uptime();

  /**
   * Set the nickname of the given client.
   *
   * @param client The client
   * @param name   The nickname
   *
   * @return The new nickname
   *
   * @throws OxClientException On errors
   */

  Optional<OxNickName> clientSetNick(
    OxServerClient client,
    OxNickName name)
    throws OxClientException;

  /**
   * Destroy (disconnect) the given client.
   *
   * @param client The client
   */

  void clientDestroy(OxServerClient client);

  /**
   * @param client The client
   *
   * @return The nick for the given client
   *
   * @throws OxNameNotRegisteredException If the client is not registered
   */

  OxNickName clientNick(OxServerClient client)
    throws OxNameNotRegisteredException;

  /**
   * @param client The client
   *
   * @return The user ID for the given client
   *
   * @throws OxNameNotRegisteredException If the client is not registered
   */

  OxUserID clientUserId(OxServerClient client)
    throws OxNameNotRegisteredException;

  /**
   * Join the client to the channel with the given name.
   *
   * @param client      The client
   * @param channelName The channel name
   *
   * @return The result of joining
   */

  OxChannelJoinResult channelJoin(
    OxServerClient client,
    OxChannelName channelName);

  /**
   * @param channelName The channel name
   *
   * @return The list of nicks present in the channel
   */

  List<OxNickName> channelNicks(
    OxChannelName channelName);

  /**
   * Part the client from the channel with the given name.
   *
   * @param client      The client
   * @param channelName The channel name
   *
   * @return The result of parting
   *
   * @throws OxClientException On errors
   */

  OxChannelPartResult channelPart(
    OxServerClient client,
    OxChannelName channelName)
    throws OxClientException;

  /**
   * @param client      The client
   * @param channelName The channel name
   *
   * @return The topic for the given channel
   */

  OxTopic channelGetTopic(
    OxServerClient client,
    OxChannelName channelName);

  /**
   * Set the topic for the given channel.
   *
   * @param client      The client
   * @param channelName The channel name
   * @param newTopic    The new topic
   *
   * @throws OxClientException On errors
   */

  void channelSetTopic(
    OxServerClient client,
    OxChannelName channelName,
    OxTopic newTopic)
    throws OxClientException;

  /**
   * @return The number of connected clients
   */

  int clientCount();

  /**
   * @return The number of channels present
   */

  int channelCount();

  /**
   * Send a channel message.
   *
   * @param client      The client
   * @param channelName The target channel
   * @param message     The message
   *
   * @throws OxClientException On errors
   */

  void channelMessage(
    OxServerClient client,
    OxChannelName channelName,
    String message)
    throws OxClientException;

  /**
   * Send a client message.
   *
   * @param client   The client
   * @param nickName The target nick
   * @param message  The message
   *
   * @throws OxClientException On errors
   */

  void clientMessage(
    OxServerClient client,
    OxNickName nickName,
    String message)
    throws OxClientException;
}
