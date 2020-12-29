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
import com.io7m.oxicoco.names.OxNickName;
import org.immutables.value.Value;

import java.util.Optional;
import java.util.UUID;

public interface OxServerEventType
{
  @ImmutablesStyleType
  @Value.Immutable
  interface OxServerClientCreatedType extends OxServerEventType
  {
    @Value.Parameter
    OxServerClient client();
  }

  @ImmutablesStyleType
  @Value.Immutable
  interface OxServerClientDestroyedType extends OxServerEventType
  {
    @Value.Parameter
    UUID clientId();
  }

  @ImmutablesStyleType
  @Value.Immutable
  interface OxServerClientNickChangedType extends OxServerEventType
  {
    OxServerClient client();

    Optional<OxNickName> oldName();

    OxNickName newName();
  }

  @ImmutablesStyleType
  @Value.Immutable
  interface OxServerChannelCreatedType extends OxServerEventType
  {
    OxChannel channel();
  }

  @ImmutablesStyleType
  @Value.Immutable
  interface OxServerChannelJoinedType extends OxServerEventType
  {
    OxServerClient client();

    OxChannel channel();
  }

  @ImmutablesStyleType
  @Value.Immutable
  interface OxServerChannelPartedType extends OxServerEventType
  {
    OxServerClient client();

    OxChannel channel();
  }
}
