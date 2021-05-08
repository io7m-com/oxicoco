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

import com.io7m.oxicoco.errors.OxIRCErrorType;
import com.io7m.oxicoco.errors.OxIRCReply;
import com.io7m.oxicoco.names.OxNickName;
import com.io7m.oxicoco.server.api.OxServerConfiguration;

import java.io.IOException;
import java.util.List;

/**
 * The execution context of a command.
 */

public interface OxServerClientCommandContextType
{
  /**
   * Send an error code to the client.
   *
   * @param error The error
   *
   * @throws IOException On I/O errors
   */

  void sendError(OxIRCErrorType error)
    throws IOException;

  /**
   * Send a command to the client.
   *
   * @param command    The command
   * @param parameters The command parameters
   * @param trailing   The trailing command information
   *
   * @throws IOException On I/O errors
   */

  void sendCommand(
    String command,
    List<String> parameters,
    String trailing)
    throws IOException;

  /**
   * Send a command to the client.
   *
   * @param command    The command
   * @param parameters The command parameters
   *
   * @throws IOException On I/O errors
   */

  default void sendCommand(
    final String command,
    final List<String> parameters)
    throws IOException
  {
    this.sendCommand(command, parameters, "");
  }

  /**
   * Send a command to the client, originating from the given user.
   *
   * @param userId     The user ID origin
   * @param trailing   The trailing command information
   * @param command    The command
   * @param parameters The command parameters
   *
   * @throws IOException On I/O errors
   */

  void sendCommandFromUser(
    OxUserID userId,
    String command,
    List<String> parameters,
    String trailing)
    throws IOException;

  /**
   * Send a command to the client, originating from the given user.
   *
   * @param userId     The user ID origin
   * @param command    The command
   * @param parameters The command parameters
   *
   * @throws IOException On I/O errors
   */

  default void sendCommandFromUser(
    final OxUserID userId,
    final String command,
    final List<String> parameters)
    throws IOException
  {
    this.sendCommandFromUser(userId, command, parameters, "");
  }

  /**
   * @return The client's user ID
   *
   * @throws OxNameNotRegisteredException If the client has not registered
   */

  default OxUserID userId()
    throws OxNameNotRegisteredException
  {
    return this.client().userId();
  }

  /**
   * @return The associated server controller
   */

  OxServerControllerType serverController();

  /**
   * @return The associated server client
   */

  OxServerClient client();

  /**
   * Send a reply to the client.
   *
   * @param reply      The reply
   * @param parameters The reply parameters
   * @param trailing   The trailing reply information
   *
   * @throws IOException On I/O errors
   */

  void sendReply(
    OxIRCReply reply,
    List<String> parameters,
    String trailing)
    throws IOException;

  /**
   * Send a reply to the client.
   *
   * @param reply      The reply
   * @param parameters The reply parameters
   *
   * @throws IOException On I/O errors
   */

  default void sendReply(
    final OxIRCReply reply,
    final List<String> parameters)
    throws IOException
  {
    this.sendReply(reply, parameters, "");
  }

  /**
   * Send a reply to the client.
   *
   * @param reply The reply
   *
   * @throws IOException On I/O errors
   */

  default void sendReply(
    final OxIRCReply reply)
    throws IOException
  {
    this.sendReply(reply, List.of(), "");
  }

  /**
   * Send a reply to the client.
   *
   * @param reply    The reply
   * @param trailing The trailing reply information
   *
   * @throws IOException On I/O errors
   */

  default void sendReply(
    final OxIRCReply reply,
    final String trailing)
    throws IOException
  {
    this.sendReply(reply, List.of(), trailing);
  }

  /**
   * @return The associated server configuration
   */

  OxServerConfiguration configuration();

  /**
   * @return The current client's nick name
   *
   * @throws OxNameNotRegisteredException If the client has no nick
   */

  default OxNickName nick()
    throws OxNameNotRegisteredException
  {
    return this.client().nick();
  }

  /**
   * Log an error.
   *
   * @param pattern The format string
   * @param e       The exception
   */

  void error(
    String pattern,
    Exception e);
}
