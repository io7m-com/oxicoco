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

package com.io7m.oxicoco.tests.server;

import com.io7m.oxicoco.names.OxServerName;
import com.io7m.oxicoco.server.api.OxServerConfiguration;
import com.io7m.oxicoco.server.api.OxServerPortConfiguration;
import com.io7m.oxicoco.server.api.OxServerType;
import com.io7m.oxicoco.server.vanilla.OxServers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class OxServerIntegrationTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(OxServerIntegrationTest.class);

  private BufferedReader inputReaderA;
  private BufferedReader inputReaderB;
  private BufferedWriter outputWriterA;
  private BufferedWriter outputWriterB;
  private OxServerType server;
  private Socket socketA;
  private Socket socketB;

  private static void send(
    final BufferedWriter writer,
    final String text)
    throws IOException
  {
    writer.write(text);
    writer.newLine();
    writer.flush();
  }

  @BeforeEach
  public void setup()
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
        .setServerName(OxServerName.of("com.example"))
        .addPorts(portConfiguration)
        .setMotd(() -> List.of("Message of the day."))
        .setBanner("oxicoco 1.0.0")
        .build();

    this.server = servers.create(configuration);
    this.server.start().get();

    this.socketA = new Socket();
    this.socketA.connect(
      new InetSocketAddress(InetAddress.getLocalHost(), 6667));
    this.socketA.setSoTimeout(1000);

    this.inputReaderA =
      new BufferedReader(
        new InputStreamReader(this.socketA.getInputStream(), UTF_8));
    this.outputWriterA =
      new BufferedWriter(
        new OutputStreamWriter(this.socketA.getOutputStream(), UTF_8));

    this.socketB = new Socket();
    this.socketB.connect(
      new InetSocketAddress(InetAddress.getLocalHost(), 6667));
    this.socketB.setSoTimeout(1000);

    this.inputReaderB =
      new BufferedReader(
        new InputStreamReader(this.socketB.getInputStream(), UTF_8));
    this.outputWriterB =
      new BufferedWriter(
        new OutputStreamWriter(this.socketB.getOutputStream(), UTF_8));
  }

  @AfterEach
  public void tearDown()
    throws IOException
  {
    this.server.close();
    this.socketA.close();
    this.socketB.close();
  }

  @Test
  public void testInvalidNick()
    throws IOException
  {
    send(this.outputWriterA, "NICK @");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 432 :invalid nickname",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testInvalidNick2()
    throws IOException
  {
    send(this.outputWriterA, "NICK");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 432 :invalid nickname",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testInvalidUser()
    throws IOException
  {
    send(this.outputWriterA, "USER @");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 400 :invalid username",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testInvalidUser2()
    throws IOException
  {
    send(this.outputWriterA, "USER");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 400 :invalid username",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testNickCollision()
    throws IOException
  {
    send(this.outputWriterA, "NICK x");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 x",
        this.inputReaderA.readLine()
      );
    });

    send(this.outputWriterB, "NICK x");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 436 x x :nickname already used",
        this.inputReaderB.readLine()
      );
    });
  }

  @Test
  public void testNickChangeOK()
    throws IOException
  {
    send(this.outputWriterA, "NICK x");
    send(this.outputWriterA, "USER x x x :Unknown");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 x",
        this.inputReaderA.readLine()
      );
    });

    send(this.outputWriterA, "NICK y");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertTrue(
        this.inputReaderA.readLine().endsWith("NICK :y")
      );
    });
  }

  @Test
  public void testNickChangeCollision()
    throws IOException
  {
    send(this.outputWriterA, "NICK x");
    send(this.outputWriterA, "USER x x x :Unknown");

    send(this.outputWriterB, "NICK y");
    send(this.outputWriterB, "USER y y y :Unknown");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 x",
        this.inputReaderA.readLine()
      );
    });

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 y",
        this.inputReaderB.readLine()
      );
    });

    send(this.outputWriterA, "NICK y");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 436 y y :nickname already used",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testRegistrationOK()
    throws IOException
  {
    send(this.outputWriterA, "NICK w");
    send(this.outputWriterA, "USER w w w :Unknown");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 w",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testBannerOK()
    throws IOException
  {
    send(this.outputWriterA, "NICK w");
    send(this.outputWriterA, "USER w w w :Unknown");
    send(this.outputWriterA, "VERSION");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 w",
        this.inputReaderA.readLine()
      );
    });

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 351 : oxicoco 1.0.0",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testStatsCOK()
    throws IOException
  {
    send(this.outputWriterA, "NICK w");
    send(this.outputWriterA, "USER w w w :Unknown");
    send(this.outputWriterA, "STATS c");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 w",
        this.inputReaderA.readLine()
      );
    });

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 244 : Clients:  2 connected",
        this.inputReaderA.readLine()
      );
    });

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 244 : Channels: 0",
        this.inputReaderA.readLine()
      );
    });

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 219",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testStatsUOK()
    throws IOException
  {
    send(this.outputWriterA, "NICK w");
    send(this.outputWriterA, "USER w w w :Unknown");
    send(this.outputWriterA, "STATS u");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 w",
        this.inputReaderA.readLine()
      );
    });

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertTrue(
        this.inputReaderA.readLine().startsWith(":com.example 242 : Uptime: PT")
      );
    });

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 219",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testStatsMiscOK()
    throws IOException
  {
    send(this.outputWriterA, "NICK w");
    send(this.outputWriterA, "USER w w w :Unknown");
    send(this.outputWriterA, "STATS");
    send(this.outputWriterA, "STATS z");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 w",
        this.inputReaderA.readLine()
      );
    });

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 219",
        this.inputReaderA.readLine()
      );
    });

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 219",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testStatsMOTDOK()
    throws IOException
  {
    send(this.outputWriterA, "NICK w");
    send(this.outputWriterA, "USER w w w :Unknown");
    send(this.outputWriterA, "MOTD");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 w",
        this.inputReaderA.readLine()
      );
    });

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 375 : com.example message of the day:",
        this.inputReaderA.readLine()
      );
    });

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 372 : Message of the day.",
        this.inputReaderA.readLine()
      );
    });

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 376",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testPingOK()
    throws IOException
  {
    send(this.outputWriterA, "NICK w");
    send(this.outputWriterA, "USER w w w :Unknown");
    send(this.outputWriterA, "PING");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 w",
        this.inputReaderA.readLine()
      );
    });

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example PONG com.example",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testQuitOK()
    throws IOException
  {
    send(this.outputWriterA, "NICK w");
    send(this.outputWriterA, "USER w w w :Unknown");
    send(this.outputWriterA, "QUIT");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 w",
        this.inputReaderA.readLine()
      );
    });

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        null,
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testChannelUsageOK()
    throws IOException
  {
    send(this.outputWriterA, "NICK x");
    send(this.outputWriterA, "USER x x x :Unknown");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 x",
        this.inputReaderA.readLine()
      );
    });

    send(this.outputWriterA, "JOIN #main");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertTrue(
        this.inputReaderA.readLine().endsWith("JOIN :#main")
      );
    });
    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 332 x #main :",
        this.inputReaderA.readLine()
      );
    });
    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 353 x = #main :x",
        this.inputReaderA.readLine()
      );
    });
    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 366 x #main",
        this.inputReaderA.readLine()
      );
    });

    send(this.outputWriterB, "NICK y");
    send(this.outputWriterB, "USER y y y :Unknown");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 y",
        this.inputReaderB.readLine()
      );
    });

    send(this.outputWriterB, "JOIN #main");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertTrue(
        this.inputReaderB.readLine().endsWith("JOIN :#main")
      );
    });
    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 332 y #main :",
        this.inputReaderB.readLine()
      );
    });
    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 353 y = #main :x",
        this.inputReaderB.readLine()
      );
    });
    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 353 y = #main :y",
        this.inputReaderB.readLine()
      );
    });
    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 366 y #main",
        this.inputReaderB.readLine()
      );
    });

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertTrue(
        this.inputReaderA.readLine().endsWith("JOIN :#main")
      );
    });

    send(this.outputWriterB, "PART #main");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertTrue(
        this.inputReaderB.readLine().endsWith("PART :#main")
      );
    });
    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertTrue(
        this.inputReaderA.readLine().endsWith("PART :#main")
      );
    });
  }

  @Test
  public void testChannelJoinBad0()
    throws IOException
  {
    send(this.outputWriterA, "NICK x");
    send(this.outputWriterA, "USER x x x :Unknown");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 x",
        this.inputReaderA.readLine()
      );
    });

    send(this.outputWriterA, "JOIN z");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 400 :invalid channel name",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testChannelJoinBad1()
    throws IOException
  {
    send(this.outputWriterA, "NICK x");
    send(this.outputWriterA, "USER x x x :Unknown");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 x",
        this.inputReaderA.readLine()
      );
    });

    send(this.outputWriterA, "JOIN");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 461 :need more parameters",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testChannelPartBad0()
    throws IOException
  {
    send(this.outputWriterA, "NICK x");
    send(this.outputWriterA, "USER x x x :Unknown");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 x",
        this.inputReaderA.readLine()
      );
    });

    send(this.outputWriterA, "PART #main");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 442 :not in channel",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testChannelPartBad1()
    throws IOException
  {
    send(this.outputWriterA, "NICK x");
    send(this.outputWriterA, "USER x x x :Unknown");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 x",
        this.inputReaderA.readLine()
      );
    });

    send(this.outputWriterA, "PART z");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 400 :invalid channel name",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testChannelPartBad2()
    throws IOException
  {
    send(this.outputWriterA, "NICK x");
    send(this.outputWriterA, "USER x x x :Unknown");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 x",
        this.inputReaderA.readLine()
      );
    });

    send(this.outputWriterA, "PART");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 461 :need more parameters",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testPrivmsgOK()
    throws IOException
  {
    send(this.outputWriterA, "NICK x");
    send(this.outputWriterA, "USER x x x :Unknown");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 x",
        this.inputReaderA.readLine()
      );
    });

    send(this.outputWriterB, "NICK y");
    send(this.outputWriterB, "USER y y y :Unknown");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 y",
        this.inputReaderB.readLine()
      );
    });

    send(this.outputWriterA, "PRIVMSG y :Hello!");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertTrue(
        this.inputReaderB.readLine().endsWith("PRIVMSG :Hello!")
      );
    });
  }

  @Test
  public void testPrivmsgNoSuchNick()
    throws IOException
  {
    send(this.outputWriterA, "NICK x");
    send(this.outputWriterA, "USER x x x :Unknown");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 x",
        this.inputReaderA.readLine()
      );
    });

    send(this.outputWriterA, "PRIVMSG y :Hello!");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 401 :no such nickname",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testPrivmsgBadNick()
    throws IOException
  {
    send(this.outputWriterA, "NICK x");
    send(this.outputWriterA, "USER x x x :Unknown");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 x",
        this.inputReaderA.readLine()
      );
    });

    send(
      this.outputWriterA,
      "PRIVMSG yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy :Hello!");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 432 :invalid nickname",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testPrivmsgNothing()
    throws IOException
  {
    send(this.outputWriterA, "NICK x");
    send(this.outputWriterA, "USER x x x :Unknown");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 x",
        this.inputReaderA.readLine()
      );
    });

    send(this.outputWriterA, "PRIVMSG");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 461 :need more parameters",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testPrivmsgNoSuchChannel()
    throws IOException
  {
    send(this.outputWriterA, "NICK x");
    send(this.outputWriterA, "USER x x x :Unknown");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 x",
        this.inputReaderA.readLine()
      );
    });

    send(this.outputWriterA, "PRIVMSG #y :Hello!");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 403 :no such channel",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testPrivmsgBadChannel()
    throws IOException
  {
    send(this.outputWriterA, "NICK x");
    send(this.outputWriterA, "USER x x x :Unknown");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 x",
        this.inputReaderA.readLine()
      );
    });

    send(
      this.outputWriterA,
      "PRIVMSG #aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa :Hello!");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 400 :invalid channel name",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testChannelTopic0()
    throws IOException
  {
    send(this.outputWriterA, "NICK x");
    send(this.outputWriterA, "USER x x x :Unknown");
    send(this.outputWriterA, "JOIN #main");

    send(this.outputWriterB, "NICK y");
    send(this.outputWriterB, "USER y y y :Unknown");
    send(this.outputWriterB, "JOIN #main");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      while (true) {
        final var line = this.inputReaderA.readLine();
        LOG.debug("line: {}", line);
        if (line.endsWith("JOIN :#main")) {
          return;
        }
      }
    });

    assertTimeout(Duration.ofSeconds(2L), () -> {
      while (true) {
        final var line = this.inputReaderB.readLine();
        LOG.debug("line: {}", line);
        if (line.endsWith("JOIN :#main")) {
          return;
        }
      }
    });

    send(this.outputWriterA, "TOPIC #main :New topic!");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      while (true) {
        final var line = this.inputReaderA.readLine();
        LOG.debug("line: {}", line);
        if (line.endsWith("TOPIC #main :New topic!")) {
          return;
        }
      }
    });
    assertTimeout(Duration.ofSeconds(2L), () -> {
      while (true) {
        final var line = this.inputReaderB.readLine();
        LOG.debug("line: {}", line);
        if (line.endsWith("TOPIC #main :New topic!")) {
          return;
        }
      }
    });

    send(this.outputWriterA, "TOPIC #main");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 332 x #main :New topic!",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testChannelTopicInvalid0()
    throws IOException
  {
    send(this.outputWriterA, "NICK x");
    send(this.outputWriterA, "USER x x x :Unknown");
    send(this.outputWriterA, "JOIN #main");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      while (true) {
        final var line = this.inputReaderA.readLine();
        LOG.debug("line: {}", line);
        if (":com.example 366 x #main".equals(line)) {
          return;
        }
      }
    });

    send(this.outputWriterA, "TOPIC x :New topic!");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 400 :invalid channel name",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testChannelTopicInvalid1()
    throws IOException
  {
    send(this.outputWriterA, "NICK x");
    send(this.outputWriterA, "USER x x x :Unknown");
    send(this.outputWriterA, "JOIN #main");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      while (true) {
        final var line = this.inputReaderA.readLine();
        LOG.debug("line: {}", line);
        if (":com.example 366 x #main".equals(line)) {
          return;
        }
      }
    });

    send(this.outputWriterA, "TOPIC");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 461 :need more parameters",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testChannelTopicInvalid2()
    throws IOException
  {
    send(this.outputWriterA, "NICK x");
    send(this.outputWriterA, "USER x x x :Unknown");
    send(this.outputWriterA, "JOIN #main");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      while (true) {
        final var line = this.inputReaderA.readLine();
        LOG.debug("line: {}", line);
        if (":com.example 366 x #main".equals(line)) {
          return;
        }
      }
    });

    final var longTopic = new StringBuilder(256);
    for (int index = 0; index < 300; ++index) {
      longTopic.append("A");
    }

    send(this.outputWriterA, "TOPIC #main :" + longTopic.toString());

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 400 :invalid topic",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testChannelTopicInvalid3()
    throws IOException
  {
    send(this.outputWriterA, "NICK x");
    send(this.outputWriterA, "USER x x x :Unknown");
    send(this.outputWriterA, "JOIN #main");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      while (true) {
        final var line = this.inputReaderA.readLine();
        LOG.debug("line: {}", line);
        if (":com.example 366 x #main".equals(line)) {
          return;
        }
      }
    });

    send(this.outputWriterA, "TOPIC #test :New topic!");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 403 :no such channel",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testMode0()
    throws IOException
  {
    send(this.outputWriterA, "NICK x");
    send(this.outputWriterA, "USER x x x :Unknown");
    send(this.outputWriterA, "MODE +i");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 x",
        this.inputReaderA.readLine()
      );
    });

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 221",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testModeInvalid()
    throws IOException
  {
    send(this.outputWriterA, "NICK x");
    send(this.outputWriterA, "USER x x x :Unknown");
    send(this.outputWriterA, "MODE");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 x",
        this.inputReaderA.readLine()
      );
    });

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 461 :need more parameters",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testCAPTooFew()
    throws IOException
  {
    send(this.outputWriterA, "CAP");
    send(this.outputWriterA, "NICK x");
    send(this.outputWriterA, "USER x x x :Unknown");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 461 :need more parameters",
        this.inputReaderA.readLine()
      );
    });

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 x",
        this.inputReaderA.readLine()
      );
    });
  }

  @Test
  public void testCAPOK()
    throws IOException
  {
    send(this.outputWriterA, "CAP LS");
    send(this.outputWriterA, "NICK x");
    send(this.outputWriterA, "USER x x x :Unknown");

    assertTimeout(Duration.ofSeconds(2L), () -> {
      assertEquals(
        ":com.example 001 x",
        this.inputReaderA.readLine()
      );
    });
  }
}
