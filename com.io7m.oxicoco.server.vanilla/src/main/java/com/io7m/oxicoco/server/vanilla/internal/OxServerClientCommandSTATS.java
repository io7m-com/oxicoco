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

import com.io7m.oxicoco.messages.OxIRCMessage;

import java.io.IOException;

import static com.io7m.oxicoco.errors.OxIRCReply.RPL_ENDOFSTATS;
import static com.io7m.oxicoco.errors.OxIRCReply.RPL_STATSGENERIC;
import static com.io7m.oxicoco.errors.OxIRCReply.RPL_STATSUPTIME;

/**
 * The STATS command.
 */

public final class OxServerClientCommandSTATS
  implements OxServerClientCommandHandlerType
{
  /**
   * The STATS command.
   */

  public OxServerClientCommandSTATS()
  {

  }

  private static void executeClients(
    final OxServerClientCommandContextType context)
    throws IOException
  {
    final var controller = context.serverController();
    final var clientCount = controller.clientCount();
    final var channelCount = controller.channelCount();
    context.sendReply(
      RPL_STATSGENERIC,
      ": Clients:  " + clientCount + " connected");
    context.sendReply(RPL_STATSGENERIC, ": Channels: " + channelCount);
    context.sendReply(RPL_ENDOFSTATS);
  }

  private static void executeUptime(
    final OxServerClientCommandContextType context)
    throws IOException
  {
    final var uptime = context.serverController().uptime();
    context.sendReply(RPL_STATSUPTIME, ": Uptime: " + uptime.toString());
    context.sendReply(RPL_ENDOFSTATS);
  }

  @Override
  public void execute(
    final OxServerClientCommandContextType context,
    final OxIRCMessage message)
    throws IOException
  {
    final var parameters = message.parameters();
    if (parameters.isEmpty()) {
      context.sendReply(RPL_ENDOFSTATS);
      return;
    }

    final var command = parameters.get(0);
    switch (command) {
      case "u": {
        executeUptime(context);
        break;
      }
      case "c": {
        executeClients(context);
        break;
      }
      default: {
        context.sendReply(RPL_ENDOFSTATS);
        return;
      }
    }
  }
}
