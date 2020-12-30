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

import com.io7m.oxicoco.errors.OxIRCErrorCommandUnknown;
import com.io7m.oxicoco.errors.OxIRCErrorType;
import com.io7m.oxicoco.errors.OxIRCReply;
import com.io7m.oxicoco.messages.OxIRCMessage;
import com.io7m.oxicoco.messages.OxIRCMessageParserFactoryType;
import com.io7m.oxicoco.messages.OxIRCMessageParserType;
import com.io7m.oxicoco.names.OxNickName;
import com.io7m.oxicoco.names.OxUserName;
import com.io7m.oxicoco.server.api.OxServerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class OxServerClient implements Closeable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(OxServerClient.class);

  private final OxServerControllerType serverController;
  private final OxServerClientID clientId;
  private final OxServerConfiguration configuration;
  private final Socket socket;
  private final SocketAddress address;
  private final OxIRCMessageParserFactoryType parsers;
  private final Map<String, OxServerClientCommandHandlerType> handlers;
  private final ConcurrentLinkedQueue<OxIRCMessage> serverMessages;
  private OxServerClientCommandContextType context;
  private volatile OxUserName user;

  public OxServerClient(
    final OxServerConfiguration inConfiguration,
    final OxIRCMessageParserFactoryType inParsers,
    final OxServerControllerType inServerController,
    final OxServerClientID inClientId,
    final Socket inSocket)
  {
    this.serverController =
      Objects.requireNonNull(inServerController, "serverController");
    this.clientId =
      Objects.requireNonNull(inClientId, "clientId");
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.parsers =
      Objects.requireNonNull(inParsers, "inParsers");
    this.socket =
      Objects.requireNonNull(inSocket, "socket");
    this.address =
      this.socket.getRemoteSocketAddress();
    this.serverMessages =
      new ConcurrentLinkedQueue<>();
    this.user =
      OxUserName.of("anonymous");

    this.handlers =
      Map.ofEntries(
        Map.entry("CAP", new OxServerClientCommandCAP()),
        Map.entry("JOIN", new OxServerClientCommandJOIN()),
        Map.entry("MODE", new OxServerClientCommandMODE()),
        Map.entry("MOTD", new OxServerClientCommandMOTD()),
        Map.entry("NICK", new OxServerClientCommandNICK()),
        Map.entry("PART", new OxServerClientCommandPART()),
        Map.entry("PING", new OxServerClientCommandPING()),
        Map.entry("PRIVMSG", new OxServerClientCommandPRIVMSG()),
        Map.entry("QUIT", new OxServerClientCommandQUIT()),
        Map.entry("STATS", new OxServerClientCommandSTATS()),
        Map.entry("TOPIC", new OxServerClientCommandTOPIC()),
        Map.entry("USER", new OxServerClientCommandUSER()),
        Map.entry("VERSION", new OxServerClientCommandVERSION())
      );
  }

  @Override
  public void close()
    throws IOException
  {
    this.socket.close();
  }

  public OxServerClientID id()
  {
    return this.clientId;
  }

  public void run()
  {
    try {
      this.info("starting");
      this.socket.setSoTimeout(16 * 10);

      final var input =
        this.socket.getInputStream();
      final var output =
        this.socket.getOutputStream();
      final var parser =
        this.parsers.create();

      this.runLoop(input, output, parser);
    } catch (final IOException e) {
      this.error("i/o error: ", e);
    } finally {
      this.serverController.clientDestroy(this);
      this.info("finished");
    }
  }

  private void info(
    final String message,
    final Object... arguments)
  {
    LOG.info(
      "[{}] {}",
      this.address,
      String.format(message, arguments)
    );
  }

  private void traceInput(
    final String message)
  {
    if (LOG.isTraceEnabled()) {
      LOG.trace(
        "[{}] → {}",
        this.address,
        message
      );
    }
  }

  private void traceOutput(
    final String message)
  {
    if (LOG.isTraceEnabled()) {
      LOG.trace(
        "[{}] ← {}",
        this.address,
        message
      );
    }
  }

  private void error(
    final String message,
    final Exception exception)
  {
    LOG.error(
      "[{}] {}",
      this.address,
      message,
      exception
    );
  }

  private void error(
    final String message,
    final Object... parameters)
  {
    LOG.error(
      "[{}] {}",
      this.address,
      String.format(message, parameters)
    );
  }

  private void runLoop(
    final InputStream input,
    final OutputStream output,
    final OxIRCMessageParserType parser)
  {
    final var lineReader =
      new BufferedReader(new InputStreamReader(input, UTF_8));
    final var lineWriter =
      new BufferedWriter(new OutputStreamWriter(output, UTF_8));

    this.context = new Context(this, lineWriter);

    try {
      while (!this.socket.isClosed()) {
        try {
          final var line = lineReader.readLine();
          if (line == null) {
            return;
          }

          this.traceInput(line);
          final var message = parser.parse(line);
          if (message.isPresent()) {
            this.handleMessage(lineWriter, message.get());
          }
        } catch (final SocketTimeoutException e) {
          // Expected
        }

        while (!this.socket.isClosed()) {
          final var serverMessage = this.serverMessages.poll();
          if (serverMessage == null) {
            break;
          }
          this.sendMessage(lineWriter, serverMessage);
        }
      }
    } catch (final SocketException e) {
      if (this.socket.isClosed()) {
        return;
      }
      this.error("socket error: ", e);
    } catch (final IOException e) {
      this.error("i/o error: ", e);
    }
  }

  private void handleMessage(
    final BufferedWriter lineWriter,
    final OxIRCMessage message)
    throws IOException
  {
    final var handler = this.handlers.get(message.command());
    if (handler != null) {
      handler.execute(this.context, lineWriter, message);
    } else {
      this.sendError(lineWriter, OxIRCErrorCommandUnknown.of(message.command()));
    }
  }

  private void sendError(
    final BufferedWriter lineWriter,
    final OxIRCErrorType error)
    throws IOException
  {
    final var message =
      error.toMessage(Optional.of(this.configuration.serverName()));

    this.sendMessage(lineWriter, message);
  }

  private void sendMessage(
    final BufferedWriter lineWriter,
    final OxIRCMessage message)
    throws IOException
  {
    final var text = message.format();
    this.traceOutput(text);

    lineWriter.append(text);
    lineWriter.append('\r');
    lineWriter.append('\n');
    lineWriter.flush();
  }

  private void sendCommandFromUser(
    final BufferedWriter lineWriter,
    final OxUserID userID,
    final String commandName,
    final List<String> parameters,
    final String trailing)
    throws IOException
  {
    final var builder = OxIRCMessage.builder();
    builder.setCommand(commandName);
    builder.setParameters(parameters);
    builder.setRawText("");
    builder.setPrefix(":" + userID.format());
    builder.setTrailing(trailing);

    this.sendMessage(lineWriter, builder.build());
  }

  private void sendReply(
    final BufferedWriter lineWriter,
    final OxIRCReply reply,
    final List<String> parameters,
    final String trailing)
    throws IOException
  {
    final var builder = OxIRCMessage.builder();
    builder.setCommand(reply.format());
    builder.setParameters(parameters);
    builder.setRawText("");
    builder.setPrefix(":" + this.configuration.serverName().value());
    builder.setTrailing(trailing);

    this.sendMessage(lineWriter, builder.build());
  }

  private void sendCommand(
    final BufferedWriter lineWriter,
    final String commandName,
    final List<String> parameters,
    final String trailing)
    throws IOException
  {
    final var builder = OxIRCMessage.builder();
    builder.setCommand(commandName);
    builder.setParameters(parameters);
    builder.setRawText("");
    builder.setPrefix(":" + this.configuration.serverName().value());
    builder.setTrailing(trailing);

    this.sendMessage(lineWriter, builder.build());
  }

  public OxNickName nick()
    throws OxNameNotRegisteredException
  {
    return this.serverController.clientNick(this);
  }

  public OxUserID userId()
    throws OxNameNotRegisteredException
  {
    return this.serverController.clientUserId(this);
  }

  public OxUserName user()
  {
    return this.user;
  }

  public String host()
  {
    return this.clientId.format();
  }

  public void enqueueMessage(
    final OxIRCMessage message)
  {
    this.serverMessages.add(message);
  }

  public void setUser(
    final OxUserName name)
  {
    this.user = Objects.requireNonNull(name, "name");
  }

  private static final class Context implements OxServerClientCommandContextType
  {
    private final OxServerClient client;
    private final BufferedWriter lineWriter;

    private Context(
      final OxServerClient inClient,
      final BufferedWriter inLineWriter)
    {
      this.client =
        Objects.requireNonNull(inClient, "client");
      this.lineWriter =
        Objects.requireNonNull(inLineWriter, "lineWriter");
    }

    @Override
    public void sendError(
      final OxIRCErrorType error)
      throws IOException
    {
      this.client.sendError(this.lineWriter, error);
    }

    @Override
    public void sendCommand(
      final String command,
      final List<String> parameters,
      final String trailing)
      throws IOException
    {
      this.client.sendCommand(
        this.lineWriter,
        command,
        parameters,
        trailing
      );
    }

    @Override
    public void sendCommandFromUser(
      final OxUserID userId,
      final String command,
      final List<String> parameters,
      final String trailing)
      throws IOException
    {
      this.client.sendCommandFromUser(
        this.lineWriter,
        userId,
        command,
        parameters,
        trailing
      );
    }

    @Override
    public OxUserID userId()
      throws OxNameNotRegisteredException
    {
      return this.client.userId();
    }

    @Override
    public OxServerControllerType serverController()
    {
      return this.client.serverController;
    }

    @Override
    public OxServerClient client()
    {
      return this.client;
    }

    @Override
    public void sendReply(
      final OxIRCReply reply,
      final List<String> parameters,
      final String trailing)
      throws IOException
    {
      this.client.sendReply(this.lineWriter, reply, parameters, trailing);
    }

    @Override
    public OxServerConfiguration configuration()
    {
      return this.client.configuration;
    }

    @Override
    public OxNickName nick()
      throws OxNameNotRegisteredException
    {
      return this.client.nick();
    }

    @Override
    public void error(
      final String pattern,
      final Exception e)
    {
      this.client.error(pattern, e);
    }
  }
}
