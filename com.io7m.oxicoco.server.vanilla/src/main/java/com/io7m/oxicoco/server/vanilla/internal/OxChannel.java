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

package com.io7m.oxicoco.server.vanilla.internal;

import com.io7m.oxicoco.names.OxChannelName;
import com.io7m.oxicoco.names.OxNickName;
import com.io7m.oxicoco.names.OxTopic;

import java.util.List;
import java.util.Objects;

/**
 * A channel on the server.
 */

public final class OxChannel
{
  private final OxServerControllerType controller;
  private final OxChannelName name;
  private volatile OxTopic topic;

  /**
   * A channel on the server.
   *
   * @param inController The server controller
   * @param inName       The channel name
   */

  public OxChannel(
    final OxServerControllerType inController,
    final OxChannelName inName)
  {
    this.controller =
      Objects.requireNonNull(inController, "inController");
    this.name =
      Objects.requireNonNull(inName, "name");
    this.topic =
      OxTopic.of("");
  }

  /**
   * @return The channel name
   */

  public OxChannelName name()
  {
    return this.name;
  }

  /**
   * @return The list of nicks in the channel
   */

  public List<OxNickName> nicks()
  {
    return this.controller.channelNicks(this.name);
  }

  /**
   * @return The channel topic
   */

  public OxTopic topic()
  {
    return this.topic;
  }

  /**
   * Set the channel topic.
   *
   * @param newTopic The new topic
   */

  public void setTopic(
    final OxTopic newTopic)
  {
    this.topic = Objects.requireNonNull(newTopic, "newTopic");
  }
}
