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
import com.io7m.oxicoco.errors.OxIRCErrorNickInvalid;
import com.io7m.oxicoco.messages.OxIRCMessage;
import com.io7m.oxicoco.names.OxChannelName;
import com.io7m.oxicoco.names.OxNickName;

import java.io.BufferedWriter;
import java.io.IOException;

public final class OxServerClientCommandPRIVMSG
  implements OxServerClientCommandHandlerType
{
  public OxServerClientCommandPRIVMSG()
  {

  }

  @Override
  public void execute(
    final OxServerClientCommandContextType context,
    final BufferedWriter lineWriter,
    final OxIRCMessage message)
    throws IOException
  {
    final var parameters = message.parameters();
    if (parameters.size() < 1) {
      context.sendError(OxIRCErrorNeedMoreParameters.builder().build());
      return;
    }

    final var target = parameters.get(0);
    if (target.startsWith("#")) {
      this.executePrivmsgChannel(context, lineWriter, target, message);
      return;
    }

    this.executePrivmsgUser(context, target, message);
  }

  private void executePrivmsgUser(
    final OxServerClientCommandContextType context,
    final String target,
    final OxIRCMessage message)
    throws IOException
  {
    final OxNickName nickName;
    try {
      nickName = OxNickName.of(target);
    } catch (final IllegalArgumentException e) {
      context.sendError(OxIRCErrorNickInvalid.builder().build());
      return;
    }

    try {
      context.serverController()
        .clientMessage(
          context.client(),
          nickName, message.trailing().substring(1)
        );
    } catch (final OxClientException e) {
      context.sendError(e.error());
    }
  }

  private void executePrivmsgChannel(
    final OxServerClientCommandContextType context,
    final BufferedWriter lineWriter,
    final String target,
    final OxIRCMessage message)
    throws IOException
  {
    final OxChannelName channelName;
    try {
      channelName = OxChannelName.of(target);
    } catch (final IllegalArgumentException e) {
      context.sendError(OxIRCErrorChannelInvalid.builder().build());
      return;
    }

    try {
      context.serverController()
        .channelMessage(
          context.client(),
          channelName, message.trailing().substring(1)
        );
    } catch (final OxClientException e) {
      context.sendError(e.error());
    }
  }
}
