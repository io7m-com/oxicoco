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

import com.io7m.oxicoco.server.api.OxServerPortConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ServerSocketFactory;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A handler for a single server port. Servers listening on multiple ports
 * (for TLS and plain text connections, for example) will have multiple handlers.
 */

public final class OxServerPortHandler implements Closeable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(OxServerPortHandler.class);

  private final OxServerControllerType controller;
  private final ServerSocketFactory serverSockets;
  private final OxServerPortConfiguration portConfiguration;
  private final AtomicBoolean closed;
  private final AtomicReference<ServerSocket> socketReference;

  /**
   * A handler for a single server port.
   *
   * @param inController    The server controller
   * @param inServerSockets The factory of sockets
   * @param inPort          The port configuration
   */

  public OxServerPortHandler(
    final OxServerControllerType inController,
    final ServerSocketFactory inServerSockets,
    final OxServerPortConfiguration inPort)
  {
    this.controller =
      Objects.requireNonNull(inController, "controller");
    this.serverSockets =
      Objects.requireNonNull(inServerSockets, "serverSockets");
    this.portConfiguration =
      Objects.requireNonNull(inPort, "port");

    this.closed =
      new AtomicBoolean(false);
    this.socketReference =
      new AtomicReference<>();
  }

  /**
   * Start the port handler.
   *
   * @param future The future that will be notified when the handler is running
   */

  public void start(
    final CompletableFuture<Void> future)
  {
    final ServerSocket socket;
    try {
      socket = this.createSocket();
    } catch (final IOException e) {
      future.completeExceptionally(e);
      return;
    }

    this.socketReference.set(socket);

    try {
      socket.setSoTimeout(1000);
    } catch (final SocketException e) {
      future.completeExceptionally(e);
      return;
    }

    future.complete(null);

    final var localAddress = socket.getLocalSocketAddress();
    LOG.info("[{}] listen", localAddress);

    try {
      while (!this.closed.get()) {
        try {
          final var clientSocket = socket.accept();
          LOG.info("[{}] connect", clientSocket.getRemoteSocketAddress());
          this.controller.clientCreate(clientSocket);
        } catch (final SocketTimeoutException e) {
          // Fine!
        } catch (final SocketException e) {
          if (socket.isClosed()) {
            // Fine!
          } else {
            LOG.error("accept: ", e);
          }
        } catch (final IOException e) {
          LOG.error("accept: ", e);
        }
      }
    } finally {
      LOG.info("[{}] closed", localAddress);
    }
  }

  private ServerSocket createSocket()
    throws IOException
  {
    final var socket = this.serverSockets.createServerSocket();
    socket.setReuseAddress(true);
    socket.bind(new InetSocketAddress(
      this.portConfiguration.address(),
      this.portConfiguration.port())
    );
    return socket;
  }

  @Override
  public void close()
    throws IOException
  {
    if (this.closed.compareAndSet(false, true)) {
      final var socket = this.socketReference.get();
      if (socket != null) {
        socket.close();
      }
    }
  }
}
