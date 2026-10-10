package org.broadinstitute.consent.http.mail;

import java.util.UUID;

/** The send id shared by the emails written during the current request. */
public final class EmailSendId {

  private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

  private EmailSendId() {}

  public static void start() {
    CURRENT.set(UUID.randomUUID());
  }

  /** Null outside a request, where each email is grouped by the older rules. */
  public static UUID current() {
    return CURRENT.get();
  }

  public static void clear() {
    CURRENT.remove();
  }
}
