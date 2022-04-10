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

import com.io7m.oxicoco.errors.OxIRCErrorChannelInvalid;
import com.io7m.oxicoco.errors.OxIRCErrorNeedMoreParameters;
import com.io7m.oxicoco.messages.OxIRCMessage;
import com.io7m.oxicoco.names.OxChannelName;

import java.io.IOException;
import java.util.List;

/**
 * The PART command.
 */

public final class OxServerClientCommandPART
  implements OxServerClientCommandHandlerType
{
  /**
   * The PART command.
   */

  public OxServerClientCommandPART()
  {

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

    final OxChannelPartResult result;
    try {
      result = context.serverController()
        .channelPart(context.client(), channelName);
    } catch (final OxClientException e) {
      context.sendError(e.error());
      return;
    }

    try {
      final var channelParted = result.channel();
      context.sendCommandFromUser(
        context.userId(),
        "PART",
        List.of(),
        ":" + channelParted.name().value()
      );
    } catch (final OxNameNotRegisteredException e) {
      context.error("name not registered: ", e);
    }
  }
}
