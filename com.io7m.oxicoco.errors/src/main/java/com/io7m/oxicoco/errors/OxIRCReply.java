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

package com.io7m.oxicoco.errors;

/**
 * The set of IRC replies.
 */

public enum OxIRCReply
{
  /**
   * RPL_WELCOME
   */

  RPL_WELCOME(1),

  /**
   * RPL_YOURHOST
   */

  RPL_YOURHOST(2),

  /**
   * RPL_CREATED
   */

  RPL_CREATED(3),

  /**
   * RPL_MYINFO
   */

  RPL_MYINFO(4),

  /**
   * RPL_BOUNCE
   */

  RPL_BOUNCE(5),

  /**
   * RPL_TRACELINK
   */

  RPL_TRACELINK(200),

  /**
   * RPL_TRACECONNECTING
   */

  RPL_TRACECONNECTING(201),

  /**
   * RPL_TRACEHANDSHAKE
   */

  RPL_TRACEHANDSHAKE(202),

  /**
   * RPL_TRACEUNKNOWN
   */

  RPL_TRACEUNKNOWN(203),

  /**
   * RPL_TRACEOPERATOR
   */

  RPL_TRACEOPERATOR(204),

  /**
   * RPL_TRACEUSER
   */

  RPL_TRACEUSER(205),

  /**
   * RPL_TRACESERVER
   */

  RPL_TRACESERVER(206),

  /**
   * RPL_TRACESERVICE
   */

  RPL_TRACESERVICE(207),

  /**
   * RPL_TRACENEWTYPE
   */

  RPL_TRACENEWTYPE(208),

  /**
   * RPL_TRACECLASS
   */

  RPL_TRACECLASS(209),

  /**
   * RPL_TRACERECONNECT
   */

  RPL_TRACERECONNECT(210),

  /**
   * RPL_STATSLINKINFO
   */

  RPL_STATSLINKINFO(211),

  /**
   * RPL_STATSCOMMANDS
   */

  RPL_STATSCOMMANDS(212),

  /**
   * RPL_ENDOFSTATS
   */

  RPL_ENDOFSTATS(219),

  /**
   * RPL_UMODEIS
   */

  RPL_UMODEIS(221),

  /**
   * RPL_SERVLIST
   */

  RPL_SERVLIST(234),

  /**
   * RPL_SERVLISTEND
   */

  RPL_SERVLISTEND(235),

  /**
   * RPL_STATSUPTIME
   */

  RPL_STATSUPTIME(242),

  /**
   * RPL_STATSOLINE
   */

  RPL_STATSOLINE(243),

  /**
   * RPL_LUSERCLIENT
   */

  RPL_LUSERCLIENT(251),

  /**
   * RPL_LUSEROP
   */

  RPL_LUSEROP(252),

  /**
   * RPL_LUSERUNKNOWN
   */

  RPL_LUSERUNKNOWN(253),

  /**
   * RPL_LUSERCHANNELS
   */

  RPL_LUSERCHANNELS(254),

  /**
   * RPL_LUSERME
   */

  RPL_LUSERME(255),

  /**
   * RPL_ADMINME
   */

  RPL_ADMINME(256),

  /**
   * RPL_ADMINLOC1
   */

  RPL_ADMINLOC1(257),

  /**
   * RPL_ADMINLOC2
   */

  RPL_ADMINLOC2(258),

  /**
   * RPL_ADMINEMAIL
   */

  RPL_ADMINEMAIL(259),

  /**
   * RPL_TRACELOG
   */

  RPL_TRACELOG(261),

  /**
   * RPL_TRACEEND
   */

  RPL_TRACEEND(262),

  /**
   * RPL_TRYAGAIN
   */

  RPL_TRYAGAIN(263),

  /**
   * RPL_AWAY
   */

  RPL_AWAY(301),

  /**
   * RPL_USERHOST
   */

  RPL_USERHOST(302),

  /**
   * RPL_ISON
   */

  RPL_ISON(303),

  /**
   * RPL_UNAWAY
   */

  RPL_UNAWAY(305),

  /**
   * RPL_NOWAWAY
   */

  RPL_NOWAWAY(306),

  /**
   * RPL_WHOISUSER
   */

  RPL_WHOISUSER(311),

  /**
   * RPL_WHOISSERVER
   */

  RPL_WHOISSERVER(312),

  /**
   * RPL_WHOISOPERATOR
   */

  RPL_WHOISOPERATOR(313),

  /**
   * RPL_WHOWASUSER
   */

  RPL_WHOWASUSER(314),

  /**
   * RPL_ENDOFWHO
   */

  RPL_ENDOFWHO(315),

  /**
   * RPL_WHOISIDLE
   */

  RPL_WHOISIDLE(317),

  /**
   * RPL_ENDOFWHOIS
   */

  RPL_ENDOFWHOIS(318),

  /**
   * RPL_WHOISCHANNELS
   */

  RPL_WHOISCHANNELS(319),

  /**
   * RPL_LISTSTART
   */

  RPL_LISTSTART(321),

  /**
   * RPL_LIST
   */

  RPL_LIST(322),

  /**
   * RPL_LISTEND
   */

  RPL_LISTEND(323),

  /**
   * RPL_CHANNELMODEIS
   */

  RPL_CHANNELMODEIS(324),

  /**
   * RPL_UNIQOPIS
   */

  RPL_UNIQOPIS(325),

  /**
   * RPL_NOTOPIC
   */

  RPL_NOTOPIC(331),

  /**
   * RPL_TOPIC
   */

  RPL_TOPIC(332),

  /**
   * RPL_INVITING
   */

  RPL_INVITING(341),

  /**
   * RPL_SUMMONING
   */

  RPL_SUMMONING(342),

  /**
   * RPL_INVITELIST
   */

  RPL_INVITELIST(346),

  /**
   * RPL_ENDOFINVITELIST
   */

  RPL_ENDOFINVITELIST(347),

  /**
   * RPL_EXCEPTLIST
   */

  RPL_EXCEPTLIST(348),

  /**
   * RPL_ENDOFEXCEPTLIST
   */

  RPL_ENDOFEXCEPTLIST(349),

  /**
   * RPL_VERSION
   */

  RPL_VERSION(351),

  /**
   * RPL_WHOREPLY
   */

  RPL_WHOREPLY(352),

  /**
   * RPL_NAMREPLY
   */

  RPL_NAMREPLY(353),

  /**
   * RPL_LINKS
   */

  RPL_LINKS(364),

  /**
   * RPL_ENDOFLINKS
   */

  RPL_ENDOFLINKS(365),

  /**
   * RPL_ENDOFNAMES
   */

  RPL_ENDOFNAMES(366),

  /**
   * RPL_BANLIST
   */

  RPL_BANLIST(367),

  /**
   * RPL_ENDOFBANLIST
   */

  RPL_ENDOFBANLIST(368),

  /**
   * RPL_ENDOFWHOWAS
   */

  RPL_ENDOFWHOWAS(369),

  /**
   * RPL_INFO
   */

  RPL_INFO(371),

  /**
   * RPL_MOTD
   */

  RPL_MOTD(372),

  /**
   * RPL_ENDOFINFO
   */

  RPL_ENDOFINFO(374),

  /**
   * RPL_MOTDSTART
   */

  RPL_MOTDSTART(375),

  /**
   * RPL_ENDOFMOTD
   */

  RPL_ENDOFMOTD(376),

  /**
   * RPL_YOUREOPER
   */

  RPL_YOUREOPER(381),

  /**
   * RPL_REHASHING
   */

  RPL_REHASHING(382),

  /**
   * RPL_YOURESERVICE
   */

  RPL_YOURESERVICE(383),

  /**
   * RPL_TIME
   */

  RPL_TIME(391),

  /**
   * RPL_USERSSTART
   */

  RPL_USERSSTART(392),

  /**
   * RPL_USERS
   */

  RPL_USERS(393),

  /**
   * RPL_ENDOFUSERS
   */

  RPL_ENDOFUSERS(394),

  /**
   * RPL_NOUSERS
   */

  RPL_NOUSERS(395),

  /**
   * Extension: Generic stats
   */

  RPL_STATSGENERIC(244);

  private final int code;

  OxIRCReply(final int inCode)
  {
    this.code = inCode;
  }

  /**
   * @return The error code integer
   */

  public int code()
  {
    return this.code;
  }

  /**
   * @return The formatted error code
   */

  public String format()
  {
    return String.format("%03d", Integer.valueOf(this.code()));
  }
}
