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

import com.io7m.oxicoco.names.OxChannelName;
import com.io7m.oxicoco.names.OxNickName;
import com.io7m.oxicoco.names.OxTopic;
import io.reactivex.rxjava3.core.Observable;

import java.io.Closeable;
import java.net.Socket;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface OxServerControllerType extends Closeable
{
  Observable<OxServerEventType> events();

  OxServerClient clientCreate(Socket socket);

  Duration uptime();

  Optional<OxNickName> clientSetNick(
    OxServerClient client,
    OxNickName name)
    throws OxClientException;

  void clientDestroy(OxServerClient client);

  OxNickName clientNick(OxServerClient client)
    throws OxNameNotRegisteredException;

  OxUserID clientUserId(OxServerClient client)
    throws OxNameNotRegisteredException;

  OxChannelJoinResult channelJoin(
    OxServerClient client,
    OxChannelName channelName);

  List<OxNickName> channelNicks(
    OxChannelName channelName);

  OxChannelPartResult channelPart(
    OxServerClient client,
    OxChannelName channelName)
    throws OxClientException;

  OxTopic channelGetTopic(
    OxServerClient client,
    OxChannelName channelName);

  void channelSetTopic(
    OxServerClient client,
    OxChannelName channelName,
    OxTopic newTopic)
    throws OxClientException;

  int clientCount();

  int channelCount();

  void channelMessage(
    OxServerClient client,
    OxChannelName channelName,
    String message)
    throws OxClientException;

  void clientMessage(
    OxServerClient client,
    OxNickName nickName,
    String message)
    throws OxClientException;
}
