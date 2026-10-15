package org.broadinstitute.consent.http.filters;

import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.util.List;

/**
 * Picks the client IP for an anonymous rate-limit key, so that a caller cannot choose its own key.
 *
 * <p>If the socket peer is a trusted proxy, the key is the {@code X-Real-IP} header. Otherwise the
 * key is the peer, and the header is ignored. In a deployed environment the peer is always the
 * Apache sidecar on loopback, so the header decides the key.
 *
 * <p>The Google load balancer sets {@code X-Real-IP} to the client address that it saw, through the
 * {@code customRequestHeaders} entry {@code X-Real-IP:{client_ip_address}} in the shared
 * terra-helmfile ingress chart's {@code BackendConfig}. Google documents that headers the load
 * balancer adds overwrite any client header with the same name, so a caller cannot spoof it. {@code
 * X-Forwarded-For} is not used: in dev on 2026-10-15 the load balancer logged the real client
 * address, but the {@code X-Forwarded-For} that reached the Apache sidecar held only a Google
 * front-end address.
 *
 * <p>Only the {@code gke} ingress class renders that {@code BackendConfig}. Behind another ingress,
 * such as the HAProxy ingress in BEEs, nothing overwrites a client-supplied {@code X-Real-IP}.
 *
 * <p>If the peer is trusted but the header is missing, repeated, or not a single IP literal, the
 * key falls back to the peer. All such requests then share one bucket, and {@link
 * KeySource#TRUSTED_PEER_WITHOUT_HEADER} in the 429 log shows the misconfiguration.
 */
public class ClientIpResolver {

  static final String UNKNOWN = "unknown";

  /** Where a rate-limit key came from. */
  public enum KeySource {
    /** The peer is not a trusted proxy, so the key is the peer. */
    UNTRUSTED_PEER,
    /** The peer is a trusted proxy, so the key is the {@code X-Real-IP} value. */
    REAL_IP_HEADER,
    /**
     * The peer is a trusted proxy, but it sent no usable {@code X-Real-IP}, so the key is the peer.
     */
    TRUSTED_PEER_WITHOUT_HEADER
  }

  /** A rate-limit key and where it came from. */
  public record ClientIp(String key, KeySource source) {}

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
   * @param realIpValues each {@code X-Real-IP} header value, in received order
   * @return the normalized client address and its source
   */
  public ClientIp resolve(String peerAddress, List<String> realIpValues) {
    InetAddress peer = parse(peerAddress);
    if (peer == null) {
      return new ClientIp(peerAddress == null ? UNKNOWN : peerAddress, KeySource.UNTRUSTED_PEER);
    }
    String peerKey = InetAddresses.toAddrString(peer);
    if (!isTrusted(peer)) {
      return new ClientIp(peerKey, KeySource.UNTRUSTED_PEER);
    }
    // The load balancer writes exactly one value. Anything else did not come from it.
    InetAddress client = realIpValues.size() == 1 ? parse(realIpValues.getFirst().strip()) : null;
    if (client == null) {
      return new ClientIp(peerKey, KeySource.TRUSTED_PEER_WITHOUT_HEADER);
    }
    return new ClientIp(InetAddresses.toAddrString(client), KeySource.REAL_IP_HEADER);
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

  // A class, not a record: a record's equals and hashCode would compare the array by identity.
  private static final class Cidr {

    private final byte[] network;
    private final int prefixLength;

    private Cidr(byte[] network, int prefixLength) {
      this.network = network;
      this.prefixLength = prefixLength;
    }

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
