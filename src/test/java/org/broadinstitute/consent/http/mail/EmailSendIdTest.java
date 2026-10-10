package org.broadinstitute.consent.http.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EmailSendIdTest {

  @AfterEach
  void clearSendId() {
    EmailSendId.clear();
  }

  @Test
  void run_gives_the_job_its_own_send_id_and_restores_none_after() {
    AtomicReference<UUID> inJob = new AtomicReference<>();

    EmailSendId.run(() -> inJob.set(EmailSendId.current()));

    assertNotNull(inJob.get());
    assertNull(EmailSendId.current());
  }

  @Test
  void run_restores_the_surrounding_send_id_when_the_job_throws() {
    EmailSendId.start();
    UUID surrounding = EmailSendId.current();
    AtomicReference<UUID> inJob = new AtomicReference<>();

    assertThrows(
        IllegalStateException.class,
        () ->
            EmailSendId.run(
                () -> {
                  inJob.set(EmailSendId.current());
                  throw new IllegalStateException();
                }));

    assertNotEquals(surrounding, inJob.get());
    assertEquals(surrounding, EmailSendId.current());
  }
}
