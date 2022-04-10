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

package com.io7m.oxicoco.server.vanilla;

import com.io7m.oxicoco.messages.OxIRCMessageParsers;
import com.io7m.oxicoco.server.api.OxServerConfiguration;
import com.io7m.oxicoco.server.api.OxServerFactoryType;
import com.io7m.oxicoco.server.api.OxServerType;
import com.io7m.oxicoco.server.vanilla.internal.OxServer;
import com.io7m.oxicoco.server.vanilla.internal.OxServerClientID;
import com.io7m.oxicoco.server.vanilla.internal.OxServerController;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;

/**
 * The default provider of servers.
 */

public final class OxServers implements OxServerFactoryType
{
  private final Clock clock;
  private final Random random;

  /**
   * The default provider of servers.
   */

  public OxServers()
  {
    this(Clock.systemUTC(), defaultRandom());
  }

  /**
   * The default provider of servers.
   *
   * @param inClock  A clock used to track time
   * @param inRandom A random number generator
   */

  public OxServers(
    final Clock inClock,
    final Random inRandom)
  {
    this.clock =
      Objects.requireNonNull(inClock, "clock");
    this.random =
      Objects.requireNonNull(inRandom, "random");
  }

  private static SecureRandom defaultRandom()
  {
    try {
      return SecureRandom.getInstanceStrong();
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public OxServerType create(
    final OxServerConfiguration configuration)
  {
    Objects.requireNonNull(configuration, "configuration");

    final var serverMain =
      Executors.newCachedThreadPool(r -> {
        final var th = new Thread(r);
        th.setName("com.io7m.oxicoco.server");
        return th;
      });

    final var serverClients =
      Executors.newCachedThreadPool(r -> {
        final var th = new Thread(r);
        th.setName(String.format(
          "com.io7m.oxicoco.server.client[%d]",
          Long.valueOf(th.getId()))
        );
        return th;
      });

    try {
      final var controller =
        new OxServerController(
          configuration,
          this.clock,
          new OxIRCMessageParsers(),
          this::randomId
        );
      return new OxServer(
        serverMain,
        serverClients,
        ServerSocketFactory.getDefault(),
        SSLContext.getDefault().getServerSocketFactory(),
        controller,
        configuration
      );
    } catch (final NoSuchAlgorithmException e) {
      throw new UnsupportedOperationException(e);
    }
  }

  private OxServerClientID randomId()
  {
    return OxServerClientID.of(this.random.nextInt());
  }
}
