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

import com.io7m.oxicoco.errors.OxIRCErrorChannelNonexistent;
import com.io7m.oxicoco.errors.OxIRCErrorChannelNotIn;
import com.io7m.oxicoco.names.OxChannelName;
import com.io7m.oxicoco.names.OxTopic;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

import static com.io7m.oxicoco.server.vanilla.internal.OxChannelJoinResultType.JoinStatus.CHANNEL_ALREADY_JOINED;
import static com.io7m.oxicoco.server.vanilla.internal.OxChannelJoinResultType.JoinStatus.CHANNEL_JOINED_CREATED;
import static com.io7m.oxicoco.server.vanilla.internal.OxChannelJoinResultType.JoinStatus.CHANNEL_JOINED_EXISTING;

/**
 * A map of channels that preserves the various invariants required by an IRC
 * server.
 */

public final class OxChannelMap
{
  private final TreeMap<OxChannelName, OxChannel> channels;
  private final HashSetValuedHashMap<OxChannelName, OxServerClientID> channelToUsers;
  private final HashSetValuedHashMap<OxServerClientID, OxChannelName> usersToChannel;

  /**
   * Create an empty channel map.
   */

  public OxChannelMap()
  {
    this.channels =
      new TreeMap<>();
    this.channelToUsers =
      new HashSetValuedHashMap<>();
    this.usersToChannel =
      new HashSetValuedHashMap<>();
  }

  /**
   * Join a client to the given channel.
   *
   * @param client          The client
   * @param channelName     The name of the channel
   * @param channelSupplier A creator of channels
   *
   * @return The result of attempting to join
   */

  public OxChannelJoinResult channelJoin(
    final OxServerClient client,
    final OxChannelName channelName,
    final Function<OxChannelName, OxChannel> channelSupplier)
  {
    final var result = OxChannelJoinResult.builder();
    result.setClient(client);

    final var existing = this.channels.get(channelName);
    if (existing == null) {
      final var channel = channelSupplier.apply(channelName);
      this.channels.put(channelName, channel);
      result.setChannel(channel);
      result.setStatus(CHANNEL_JOINED_CREATED);
    } else {
      result.setChannel(existing);
      result.setStatus(CHANNEL_JOINED_EXISTING);
    }

    if (this.channelToUsers.containsMapping(channelName, client.id())) {
      result.setStatus(CHANNEL_ALREADY_JOINED);
    } else {
      result.setNotifyUsers(
        Optional.ofNullable(this.channelToUsers.get(channelName))
          .orElse(Set.of())
      );
    }

    this.channelToUsers.put(channelName, client.id());
    this.usersToChannel.put(client.id(), channelName);
    return result.build();
  }

  /**
   * @param channelName The channel name
   *
   * @return The set of clients present in the channel
   */

  public Set<OxServerClientID> channelClients(
    final OxChannelName channelName)
  {
    return this.channelToUsers.get(channelName);
  }

  /**
   * Part a client from the given channel.
   *
   * @param client      The client
   * @param channelName The name of the channel
   *
   * @return The result of attempting to part
   *
   * @throws OxClientException On errors
   */

  public OxChannelPartResult channelPart(
    final OxServerClient client,
    final OxChannelName channelName)
    throws OxClientException
  {
    final var result = OxChannelPartResult.builder();
    result.setClient(client);

    final var existing = this.channels.get(channelName);
    if (existing == null) {
      throw new OxClientException(OxIRCErrorChannelNotIn.builder().build());
    }

    result.setChannel(existing);

    final var joined =
      this.channelToUsers.containsMapping(channelName, client.id());
    final var users =
      this.channelToUsers.get(channelName);

    this.channelToUsers.removeMapping(channelName, client.id());
    this.usersToChannel.removeMapping(client.id(), channelName);

    result.setParted(joined);
    result.setNotifyUsers(users);
    return result.build();
  }

  /**
   * @param channelName The channel name
   *
   * @return The topic of the given channel
   */

  public OxTopic channelTopic(
    final OxChannelName channelName)
  {
    final var existing = this.channels.get(channelName);
    if (existing == null) {
      return OxTopic.of("");
    }
    return existing.topic();
  }

  /**
   * Set the topic for the given channel.
   *
   * @param channelName The channel name
   * @param newTopic    The new topic
   *
   * @return The channel
   *
   * @throws OxClientException On errors
   */

  public OxChannel channelTopicSet(
    final OxChannelName channelName,
    final OxTopic newTopic)
    throws OxClientException
  {
    final var existing = this.channels.get(channelName);
    if (existing == null) {
      throw new OxClientException(OxIRCErrorChannelNonexistent.builder().build());
    }

    existing.setTopic(newTopic);
    return existing;
  }

  /**
   * @return The number of channels present
   */

  public int channelCount()
  {
    return this.channels.size();
  }

  /**
   * @param client The client
   *
   * @return The set of channels to which the client is joined
   */

  public Set<OxChannelName> channelsFor(
    final OxServerClient client)
  {
    return this.usersToChannel.get(client.id());
  }

  /**
   * @param channelName The channel name
   *
   * @return The channel with the given name
   */

  public Optional<OxChannel> channelOf(
    final OxChannelName channelName)
  {
    return Optional.ofNullable(this.channels.get(channelName));
  }
}
