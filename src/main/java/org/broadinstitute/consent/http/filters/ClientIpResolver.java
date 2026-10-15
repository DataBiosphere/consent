package org.broadinstitute.consent.http.filters;

import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Picks the client IP for an anonymous rate-limit key, so that a caller cannot choose its own key.
 *
 * <p>If the socket peer is not a trusted proxy, the result is the peer, and {@code X-Forwarded-For}
 * is ignored. If the peer is trusted, the header is read from the right. Each trusted hop appends
 * the address it received the request from, so the first entry from the right that is not a trusted
 * proxy is the client. A caller can write any entries at the left of the header, but those entries
 * are never reached while a trusted hop appended a real address to their right.
 *
 * <p>Behind the Google load balancer and the Apache sidecar, the header that Consent receives is
 * {@code <caller entries>, <client>, <load balancer address>, <front-end address>}. Requests from
 * the BFF add the BFF egress address after the browser address. Each of those infrastructure
 * addresses must be a trusted proxy for the key to be the browser address.
 */
public class ClientIpResolver {

  static final String UNKNOWN = "unknown";

  private final List<Cidr> trustedProxies;

  /**
   * @param trustedProxies IP address literals or CIDR ranges; hostnames are rejected
   * @throws IllegalArgumentException if an entry is not an IP literal or valid CIDR range
   */
  public ClientIpResolver(List<String> trustedProxies) {
    this.trustedProxies = trustedProxies.stream().map(Cidr::parse).toList();
  }

  /**
   * @param peerAddress the socket peer address
   * @param forwardedForValues each {@code X-Forwarded-For} header value, in received order
   * @return the normalized client address
   */
  public String resolve(String peerAddress, List<String> forwardedForValues) {
    InetAddress peer = parse(peerAddress);
    if (peer == null) {
      return peerAddress == null ? UNKNOWN : peerAddress;
    }
    if (!isTrusted(peer)) {
      return InetAddresses.toAddrString(peer);
    }
    List<String> entries = new ArrayList<>();
    for (String value : forwardedForValues) {
      for (String entry : value.split(",")) {
        if (!entry.isBlank()) {
          entries.add(entry.strip());
        }
      }
    }
    InetAddress candidate = peer;
    for (int i = entries.size() - 1; i >= 0; i--) {
      InetAddress hop = parse(entries.get(i));
      if (hop == null) {
        // A trusted hop wrote something that is not an address. Do not guess past it.
        return InetAddresses.toAddrString(peer);
      }
      candidate = hop;
      if (!isTrusted(hop)) {
        break;
      }
    }
    return InetAddresses.toAddrString(candidate);
  }

  private boolean isTrusted(InetAddress address) {
    return trustedProxies.stream().anyMatch(cidr -> cidr.contains(address));
  }

  private static InetAddress parse(String value) {
    if (value == null || !InetAddresses.isInetAddress(value)) {
      return null;
    }
    return InetAddresses.forString(value);
  }

  private record Cidr(byte[] network, int prefixLength) {

    static Cidr parse(String value) {
      String[] parts = value.strip().split("/", 2);
      InetAddress address = InetAddresses.forString(parts[0]);
      byte[] bytes = address.getAddress();
      int maxPrefix = bytes.length * 8;
      int prefix;
      try {
        prefix = parts.length == 2 ? Integer.parseInt(parts[1]) : maxPrefix;
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid CIDR prefix: " + value, e);
      }
      if (prefix < 0 || prefix > maxPrefix) {
        throw new IllegalArgumentException("Invalid CIDR prefix: " + value);
      }
      return new Cidr(bytes, prefix);
    }

    boolean contains(InetAddress address) {
      byte[] bytes = address.getAddress();
      if (bytes.length != network.length) {
        return false;
      }
      int fullBytes = prefixLength / 8;
      for (int i = 0; i < fullBytes; i++) {
        if (bytes[i] != network[i]) {
          return false;
        }
      }
      int remainingBits = prefixLength % 8;
      if (remainingBits == 0) {
        return true;
      }
      int mask = (0xFF << (8 - remainingBits)) & 0xFF;
      return (bytes[fullBytes] & mask) == (network[fullBytes] & mask);
    }
  }
}
