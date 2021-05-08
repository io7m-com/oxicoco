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

import com.io7m.oxicoco.errors.OxIRCErrorChannelInvalid;
import com.io7m.oxicoco.errors.OxIRCErrorNeedMoreParameters;
import com.io7m.oxicoco.errors.OxIRCErrorTopicInvalid;
import com.io7m.oxicoco.messages.OxIRCMessage;
import com.io7m.oxicoco.names.OxChannelName;
import com.io7m.oxicoco.names.OxTopic;

import java.io.IOException;
import java.util.List;

import static com.io7m.oxicoco.errors.OxIRCReply.RPL_TOPIC;

/**
 * The TOPIC command.
 */

public final class OxServerClientCommandTOPIC
  implements OxServerClientCommandHandlerType
{
  /**
   * The TOPIC command.
   */

  public OxServerClientCommandTOPIC()
  {

  }

  private static void handleTOPICMessageSetNew(
    final OxServerClientCommandContextType context,
    final OxChannelName channelName,
    final String newTopic)
    throws IOException
  {
    try {
      final var topicText = newTopic.substring(1);
      final var topic = OxTopic.of(topicText);

      context.serverController()
        .channelSetTopic(context.client(), channelName, topic);

      context.sendCommandFromUser(
        context.userId(),
        "TOPIC",
        List.of(channelName.value()),
        ":" + topicText
      );
    } catch (final OxNameNotRegisteredException e) {
      context.error("name not registered: ", e);
    } catch (final IllegalArgumentException e) {
      context.sendError(OxIRCErrorTopicInvalid.builder().build());
    } catch (final OxClientException e) {
      context.sendError(e.error());
    }
  }

  private static void handleTOPICMessageGet(
    final OxServerClientCommandContextType context,
    final OxChannelName channelName)
    throws IOException
  {
    final var result =
      context.serverController().channelGetTopic(context.client(), channelName);

    try {
      final var currentNick = context.nick();
      context.sendReply(
        RPL_TOPIC,
        List.of(currentNick.value(), channelName.value()),
        ":" + result.value()
      );
    } catch (final OxNameNotRegisteredException e) {
      context.error("name not registered: ", e);
    }
  }

  @Override
  public void execute(
    final OxServerClientCommandContextType context,
    final OxIRCMessage message)
    throws IOException
  {
    final var parameters = message.parameters();
    if (parameters.size() < 1) {
      context.sendError(OxIRCErrorNeedMoreParameters.builder().build());
      return;
    }

    final var channel = parameters.get(0);
    final OxChannelName channelName;

    try {
      channelName = OxChannelName.of(channel);
    } catch (final IllegalArgumentException e) {
      context.sendError(OxIRCErrorChannelInvalid.builder().build());
      return;
    }

    final var newTopic = message.trailing();
    if (!newTopic.isEmpty()) {
      handleTOPICMessageSetNew(context, channelName, newTopic);
      return;
    }

    handleTOPICMessageGet(context, channelName);
  }
}
