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

package com.io7m.oxicoco.tests;

import com.io7m.oxicoco.names.OxServerName;
import com.io7m.oxicoco.server.api.OxServerConfiguration;
import com.io7m.oxicoco.server.api.OxServerPortConfiguration;
import com.io7m.oxicoco.server.vanilla.OxServers;

import java.net.InetAddress;
import java.util.List;

public final class ServerMain
{
  private ServerMain()
  {

  }

  public static void main(
    final String[] args)
    throws Exception
  {
    final var servers = new OxServers();

    final var portConfiguration =
      OxServerPortConfiguration.builder()
        .setAddress(InetAddress.getLocalHost())
        .setPort(6667)
        .setEnableTLS(false)
        .build();

    final var configuration =
      OxServerConfiguration.builder()
        .setServerName(OxServerName.of("info.arc7.sunflower"))
        .addPorts(portConfiguration)
        .setMotd(() -> List.of("Message of the day."))
        .setBanner("oxicoco 1.0.0")
        .build();

    try (var server = servers.create(configuration)) {
      server.start().get();
      System.in.read();
    }
  }
}
