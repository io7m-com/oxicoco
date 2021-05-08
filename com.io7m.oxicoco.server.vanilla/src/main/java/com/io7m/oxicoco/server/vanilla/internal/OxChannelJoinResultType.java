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

import com.io7m.immutables.styles.ImmutablesStyleType;
import org.immutables.value.Value;

import java.util.Set;

/**
 * The result of joining a channel.
 */

@ImmutablesStyleType
@Value.Immutable
public interface OxChannelJoinResultType
{
  /**
   * @return The client
   */

  OxServerClient client();

  /**
   * @return The channel
   */

  OxChannel channel();

  /**
   * @return The join status
   */

  JoinStatus status();

  /**
   * @return The set of users that should be notified
   */

  Set<OxServerClientID> notifyUsers();

  /**
   * The different types of join status.
   */

  enum JoinStatus
  {
    /**
     * The user was already joined to the channel.
     */

    CHANNEL_ALREADY_JOINED,

    /**
     * The user joined an existing channel.
     */

    CHANNEL_JOINED_EXISTING,

    /**
     * The user created a channel by joining it.
     */

    CHANNEL_JOINED_CREATED
  }
}
