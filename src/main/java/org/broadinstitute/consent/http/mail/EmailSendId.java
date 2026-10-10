package org.broadinstitute.consent.http.mail;

import java.util.UUID;

/** The send id shared by the emails written during the current request. */
public final class EmailSendId {

  private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

  private EmailSendId() {}

  public static void start() {
    CURRENT.set(UUID.randomUUID());
  }

  /** Runs a job off the request thread, such as a scheduled batch, under a send id of its own. */
  public static void run(Runnable job) {
    UUID previous = CURRENT.get();
    CURRENT.set(UUID.randomUUID());
    try {
      job.run();
    } finally {
      CURRENT.set(previous);
    }
  }

  /** Null outside a request or job, where each email is grouped by the older rules. */
  public static UUID current() {
    return CURRENT.get();
  }

  public static void clear() {
    CURRENT.remove();
  }
}
