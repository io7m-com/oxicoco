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

package com.io7m.oxicoco.messages;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * A factory of message parsers.
 */

public final class OxIRCMessageParsers implements OxIRCMessageParserFactoryType
{
  /**
   * A factory of message parsers.
   */

  public OxIRCMessageParsers()
  {

  }


  @Override
  public OxIRCMessageParserType create()
  {
    return new Parser();
  }

  private final class Parser implements OxIRCMessageParserType
  {
    Parser()
    {

    }

    @Override
    public Optional<OxIRCMessage> parse(
      final String line)
    {
      Objects.requireNonNull(line, "line");

      if (line.isBlank()) {
        return Optional.empty();
      }

      final var messageBuilder = OxIRCMessage.builder();
      messageBuilder.setRawText(line);
      messageBuilder.setTrailing("");
      messageBuilder.setPrefix("");

      var lineNow = line;
      if (lineNow.startsWith("@")) {
        lineNow = this.consumeTags(messageBuilder, lineNow);
      }

      lineNow = lineNow.stripLeading();
      if (lineNow.startsWith(":")) {
        lineNow = this.consumePrefix(messageBuilder, lineNow);
      }

      lineNow = lineNow.stripLeading();
      lineNow = this.consumeCommand(messageBuilder, lineNow);
      lineNow = lineNow.stripLeading();

      if (!lineNow.isEmpty()) {
        this.consumeParameters(messageBuilder, lineNow);
      }
      return Optional.of(messageBuilder.build());
    }

    private String consumeParameters(
      final OxIRCMessage.Builder messageBuilder,
      final String line)
    {
      if (line.startsWith(":")) {
        return this.consumeTrailing(messageBuilder, line);
      }

      final var end = line.indexOf((int) ' ');
      if (end == -1) {
        messageBuilder.addParameters(line);
        return "";
      }

      final var text = line.substring(0, end);
      messageBuilder.addParameters(text);
      return this.consumeParameters(
        messageBuilder,
        line.substring(end).stripLeading());
    }

    private String consumeTrailing(
      final OxIRCMessage.Builder messageBuilder,
      final String line)
    {
      messageBuilder.setTrailing(line);
      return line;
    }

    private String consumeCommand(
      final OxIRCMessage.Builder messageBuilder,
      final String line)
    {
      final var end = line.indexOf((int) ' ');
      if (end == -1) {
        messageBuilder.setCommand(line.toUpperCase(Locale.ROOT));
        return "";
      }

      final var command = line.substring(0, end);
      messageBuilder.setCommand(command.toUpperCase(Locale.ROOT));
      return line.substring(end);
    }

    private String consumePrefix(
      final OxIRCMessage.Builder messageBuilder,
      final String line)
    {
      final var end = line.indexOf((int) ' ');
      final var prefix = line.substring(0, end);
      messageBuilder.setPrefix(prefix);
      return line.substring(end);
    }

    private String consumeTags(
      final OxIRCMessage.Builder messageBuilder,
      final String line)
    {
      final var end = line.indexOf((int) ' ');
      return line.substring(end);
    }
  }
}
