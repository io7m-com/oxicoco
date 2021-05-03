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

import com.io7m.immutables.styles.ImmutablesStyleType;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * An IRC message.
 */

@ImmutablesStyleType
@Value.Immutable
public interface OxIRCMessageType
{
  private static String formatEntry(
    final Map.Entry<String, String> entry)
  {
    final var key = entry.getKey();
    final var val = entry.getValue();
    if (val.isEmpty()) {
      return key;
    }
    return String.format("%s=%s", key, val);
  }

  /**
   * @return The raw message text
   */

  String rawText();

  /**
   * @return The message tags
   */

  Map<String, String> tags();

  /**
   * @return The message prefix
   */

  String prefix();

  /**
   * @return The message command
   */

  String command();

  /**
   * @return The message parameters
   */

  List<String> parameters();

  /**
   * @return The message trailing text
   */

  String trailing();

  /**
   * @return The formatted message
   */

  default String format()
  {
    final var builder = new StringBuilder(128);
    final var tagMap = this.tags();
    if (!tagMap.isEmpty()) {
      final var tagsFormatted =
        tagMap.entrySet()
          .stream()
          .map(OxIRCMessageType::formatEntry)
          .collect(Collectors.joining(";"));

      builder.append('@');
      builder.append(tagsFormatted);
      builder.append(' ');
    }

    if (!this.prefix().isEmpty()) {
      builder.append(this.prefix());
      builder.append(' ');
    }

    builder.append(this.command());

    if (!this.parameters().isEmpty()) {
      builder.append(' ');
      builder.append(String.join(" ", this.parameters()));
    }

    if (!this.trailing().isEmpty()) {
      builder.append(' ');
      builder.append(this.trailing());
    }
    return builder.toString();
  }
}
