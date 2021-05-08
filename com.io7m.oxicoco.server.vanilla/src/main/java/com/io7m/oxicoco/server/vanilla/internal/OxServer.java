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

import com.io7m.oxicoco.server.api.OxServerConfiguration;
import com.io7m.oxicoco.server.api.OxServerPortConfiguration;
import com.io7m.oxicoco.server.api.OxServerType;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * The base server implementation.
 */

public final class OxServer implements OxServerType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(OxServer.class);

  private final ExecutorService serverMain;
  private final ExecutorService serverClients;
  private final ServerSocketFactory plainServerSockets;
  private final SSLServerSocketFactory sslServerSockets;
  private final OxServerControllerType serverController;
  private final OxServerConfiguration configuration;
  private final List<OxServerPortHandler> portHandlers;
  private final CompositeDisposable subscriptions;

  /**
   * The base server implementation.
   *
   * @param inServerMain       The main server executor
   * @param inServerClients    The executor used for clients
   * @param inServerSockets    The socket factory for clients
   * @param inSSLServerSockets The SSL socket factory
   * @param inServerController The main server controller
   * @param inConfiguration    The server configuration
   */

  public OxServer(
    final ExecutorService inServerMain,
    final ExecutorService inServerClients,
    final ServerSocketFactory inServerSockets,
    final SSLServerSocketFactory inSSLServerSockets,
    final OxServerControllerType inServerController,
    final OxServerConfiguration inConfiguration)
  {
    this.serverMain =
      Objects.requireNonNull(inServerMain, "serverMain");
    this.serverClients =
      Objects.requireNonNull(inServerClients, "serverClients");
    this.plainServerSockets =
      Objects.requireNonNull(inServerSockets, "inServerSockets");
    this.sslServerSockets =
      Objects.requireNonNull(inSSLServerSockets, "SSLServerSockets");
    this.serverController =
      Objects.requireNonNull(inServerController, "serverController");
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");

    this.subscriptions = new CompositeDisposable();
    this.subscriptions.add(
      this.serverController.events()
        .ofType(OxServerClientCreated.class)
        .subscribe(this::onClientCreated, OxServer::onErrorMessage)
    );

    this.portHandlers =
      this.configuration.ports()
        .stream()
        .map(this::createPortHandler)
        .collect(Collectors.toList());
  }

  private static void onErrorMessage(
    final Throwable throwable)
  {
    LOG.error("exception: ", throwable);
  }

  private void onClientCreated(
    final OxServerClientCreated event)
  {
    this.serverClients.execute(() -> event.client().run());
  }

  private OxServerPortHandler createPortHandler(
    final OxServerPortConfiguration port)
  {
    return new OxServerPortHandler(
      this.serverController,
      port.enableTLS() ? this.sslServerSockets : this.plainServerSockets,
      port
    );
  }

  @Override
  public CompletableFuture<Void> start()
  {
    return CompletableFuture.allOf(
      this.portHandlers.stream()
        .map(this::startPortHandler)
        .toArray(CompletableFuture[]::new)
    );
  }

  private CompletableFuture<Void> startPortHandler(
    final OxServerPortHandler port)
  {
    final var future = new CompletableFuture<Void>();
    this.serverMain.execute(() -> {
      try {
        port.start(future);
      } catch (final Exception e) {
        future.completeExceptionally(e);
      }
    });
    return future;
  }

  @Override
  public void close()
    throws IOException
  {
    final var ex = new OxExceptionTracker<IOException>();
    for (final var port : this.portHandlers) {
      try {
        port.close();
      } catch (final Exception e) {
        ex.addException(new IOException(e));
      }
    }

    try {
      this.serverController.close();
    } catch (final IOException e) {
      ex.addException(e);
    }

    this.serverClients.shutdown();
    this.serverMain.shutdown();
    ex.throwIfNecessary();
  }
}
