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

import com.io7m.oxicoco.errors.OxIRCErrorChannelInvalid;
import com.io7m.oxicoco.errors.OxIRCErrorChannelNonexistent;
import com.io7m.oxicoco.errors.OxIRCErrorChannelNotIn;
import com.io7m.oxicoco.errors.OxIRCErrorCommandUnknown;
import com.io7m.oxicoco.errors.OxIRCErrorNeedMoreParameters;
import com.io7m.oxicoco.errors.OxIRCErrorNickCollision;
import com.io7m.oxicoco.errors.OxIRCErrorNickInvalid;
import com.io7m.oxicoco.errors.OxIRCErrorNickNonexistent;
import com.io7m.oxicoco.errors.OxIRCErrorTopicInvalid;
import com.io7m.oxicoco.errors.OxIRCErrorUserInvalid;
import com.io7m.oxicoco.messages.OxIRCMessage;
import com.io7m.oxicoco.names.OxChannelName;
import com.io7m.oxicoco.names.OxNickName;
import com.io7m.oxicoco.names.OxServerName;
import com.io7m.oxicoco.names.OxTopic;
import com.io7m.oxicoco.names.OxUserName;
import com.io7m.oxicoco.server.api.OxServerConfiguration;
import com.io7m.oxicoco.server.api.OxServerPortConfiguration;
import com.io7m.oxicoco.server.vanilla.internal.OxChannelJoinResult;
import com.io7m.oxicoco.server.vanilla.internal.OxChannelPartResult;
import com.io7m.oxicoco.server.vanilla.internal.OxServerChannelCreated;
import com.io7m.oxicoco.server.vanilla.internal.OxServerChannelJoined;
import com.io7m.oxicoco.server.vanilla.internal.OxServerChannelParted;
import com.io7m.oxicoco.server.vanilla.internal.OxServerClientCreated;
import com.io7m.oxicoco.server.vanilla.internal.OxServerClientDestroyed;
import com.io7m.oxicoco.server.vanilla.internal.OxServerClientNickChanged;
import com.io7m.oxicoco.server.vanilla.internal.OxUserID;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.mockito.Mockito;

import java.io.FileDescriptor;
import java.net.Socket;
import java.util.stream.Stream;

public final class OxEqualsTest
{

  private static DynamicTest toTest(
    final Class<?> clazz)
  {
    return DynamicTest.dynamicTest(
      String.format("testEquals_%s", clazz.getCanonicalName()),
      () -> {
        final var socket0 = Mockito.mock(Socket.class);
        final var socket1 = Mockito.mock(Socket.class);
        EqualsVerifier.forClass(clazz)
          .suppress(Warning.NULL_FIELDS)
          .withPrefabValues(Socket.class, socket0, socket1)
          .verify();
      }
    );
  }

  @TestFactory
  public Stream<DynamicTest> testEquals()
  {
    return Stream.of(
      OxIRCErrorCommandUnknown.class,
      OxIRCErrorChannelInvalid.class,
      OxIRCErrorChannelNonexistent.class,
      OxIRCErrorChannelNotIn.class,
      OxIRCErrorNeedMoreParameters.class,
      OxIRCErrorNickCollision.class,
      OxIRCErrorNickInvalid.class,
      OxIRCErrorNickNonexistent.class,
      OxIRCErrorTopicInvalid.class,
      OxIRCErrorUserInvalid.class,
      OxIRCMessage.class,
      OxChannelName.class,
      OxNickName.class,
      OxServerName.class,
      OxTopic.class,
      OxUserName.class,
      OxServerConfiguration.class,
      OxServerPortConfiguration.class,
      OxChannelJoinResult.class,
      OxChannelPartResult.class,
      OxServerChannelCreated.class,
      OxServerChannelJoined.class,
      OxServerChannelParted.class,
      OxServerClientCreated.class,
      OxServerClientDestroyed.class,
      OxServerClientNickChanged.class,
      OxUserID.class
    ).map(OxEqualsTest::toTest);
  }
}
