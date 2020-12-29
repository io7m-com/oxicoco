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

package com.io7m.oxicoco.tests.messages;

import com.io7m.oxicoco.messages.OxIRCMessageParserType;
import com.io7m.oxicoco.messages.OxIRCMessageParsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class OxIRCMessageParsersTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(OxIRCMessageParsersTest.class);

  private OxIRCMessageParsers parsers;
  private OxIRCMessageParserType parser;

  @BeforeEach
  public void setup()
  {
    this.parsers = new OxIRCMessageParsers();
    this.parser = this.parsers.create();
  }

  @Test
  public void testEmpty()
  {
    final var text = "   ";
    final var message = this.parser.parse(text);
    assertTrue(message.isEmpty());
  }

  @Test
  public void testNOTICE0()
  {
    final var text = ":irc.example.com NOTICE * :*** Looking up your hostname...";
    final var message = this.parser.parse(text).orElseThrow();

    LOG.debug("message: {}", message);
    assertEquals("NOTICE", message.command());
    assertEquals("*", message.parameters().get(0));
    assertEquals(":*** Looking up your hostname...", message.trailing());
    assertEquals(":irc.example.com", message.prefix());
    assertEquals(text, message.rawText());
    LOG.debug("message.raw:    {}", message.rawText());
    LOG.debug("message.format: {}", message.format());
  }

  @Test
  public void testNOTICE1()
  {
    final var text = ":irc.example.com NOTICE * :*** Could not resolve your hostname: Domain not found; using your IP address (2606:2800:220:1:248:1893:25c8:1946) instead.";
    final var message = this.parser.parse(text).orElseThrow();

    LOG.debug("message: {}", message);
    assertEquals("NOTICE", message.command());
    assertEquals("*", message.parameters().get(0));
    assertEquals(":*** Could not resolve your hostname: Domain not found; using your IP address (2606:2800:220:1:248:1893:25c8:1946) instead.", message.trailing());
    assertEquals(":irc.example.com", message.prefix());
    assertEquals(text, message.rawText());
    LOG.debug("message.raw:    {}", message.rawText());
    LOG.debug("message.format: {}", message.format());
  }

  @Test
  public void testCAPLS()
  {
    final var text = "CAP LS";
    final var message = this.parser.parse(text).orElseThrow();

    LOG.debug("message: {}", message);
    assertEquals("CAP", message.command());
    assertEquals("LS", message.parameters().get(0));
    assertEquals("", message.trailing());
    assertEquals("", message.prefix());
    assertEquals(text, message.rawText());
    LOG.debug("message.raw:    {}", message.rawText());
    LOG.debug("message.format: {}", message.format());
  }

  @Test
  public void testMOTD()
  {
    final var text = "MOTD";
    final var message = this.parser.parse(text).orElseThrow();

    LOG.debug("message: {}", message);
    assertEquals("MOTD", message.command());
    assertEquals(0, message.parameters().size());
    assertEquals("", message.trailing());
    assertEquals("", message.prefix());
    assertEquals(text, message.rawText());
    LOG.debug("message.raw:    {}", message.rawText());
    LOG.debug("message.format: {}", message.format());
  }

  @Test
  public void testMOTDLow()
  {
    final var text = "motd";
    final var message = this.parser.parse(text).orElseThrow();

    LOG.debug("message: {}", message);
    assertEquals("MOTD", message.command());
    assertEquals(0, message.parameters().size());
    assertEquals("", message.trailing());
    assertEquals("", message.prefix());
    assertEquals(text, message.rawText());
    LOG.debug("message.raw:    {}", message.rawText());
    LOG.debug("message.format: {}", message.format());
  }
}
