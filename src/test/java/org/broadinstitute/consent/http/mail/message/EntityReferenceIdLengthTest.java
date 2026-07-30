package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.reflect.ClassPath;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.Reminder;
import org.broadinstitute.consent.http.models.StudyDatasetCountRecord;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * EmailService persists every sent message into email_entity, whose entity_reference_id column is
 * varchar(255) (see MailMessageDAOTest#testInsert_EntityReferenceIdLongerThanColumnFails, which
 * pins that width against the migrated schema). The message is handed to SendGrid before the row is
 * written, so a reference id longer than the column does not fail cleanly: the email goes out and
 * only then does the insert blow up, leaving no record that it was ever sent.
 *
 * <p>Each MailMessage implementation must therefore key its reference id on a value that cannot
 * outgrow the column - an id, a uuid, a generated code, or free text that is itself bounded by a
 * varchar(255) column. Keying on free text from an unbounded (TEXT) column is the defect this class
 * guards against; study.name is TEXT, which is why NewStudyRegistrationConfirmationMessage keys on
 * the study uuid rather than the study name.
 *
 * <p>Every case below feeds the message the longest value its reference id source can hold, so a
 * message that decorates, prefixes, or concatenates its source values fails here even though each
 * individual source fits. Sources narrower than the column - a formatted digest date, a uuid, an
 * election id - are still fed the full width where the constructor takes a String, which can only
 * make the assertion stricter.
 */
class EntityReferenceIdLengthTest {

  /** Width of email_entity.entity_reference_id. */
  private static final int ENTITY_REFERENCE_ID_COLUMN_WIDTH = 255;

  /**
   * The longest value any varchar(255) source column can hold. Every column currently feeding a
   * reference id - data_access_request.reference_id, dar_collection.dar_code, dataset.name,
   * dac.name, users.email - is varchar(255).
   */
  private static final String LONGEST_BOUNDED_SOURCE = "a".repeat(255);

  /** A value only an unbounded TEXT column, such as study.name, can hold. */
  private static final String UNBOUNDED_TEXT_SOURCE = "b".repeat(1000);

  private static final List<DatasetMailDTO> DATASET_DTOS =
      List.of(new DatasetMailDTO("dataset name", "DUOS-000001", "http://dataLocationUrl"));

  private record MessageCase(
      Class<? extends MailMessage> messageType, String referenceIdSource, MailMessage message) {

    @Override
    public String toString() {
      return "%s keyed on %s".formatted(messageType.getSimpleName(), referenceIdSource);
    }
  }

  private static MessageCase messageCase(String referenceIdSource, MailMessage message) {
    return new MessageCase(message.getClass(), referenceIdSource, message);
  }

  private static User user() {
    User user = new User();
    user.setUserId(1);
    user.setDisplayName("Test User");
    user.setEmail("test@user.com");
    return user;
  }

  private static User userWithLongestEmail() {
    User user = user();
    // users.email is varchar(255)
    user.setEmail(LONGEST_BOUNDED_SOURCE);
    return user;
  }

  private static Vote voteWithLargestElectionId() {
    Vote vote = new Vote();
    vote.setVoteId(1);
    vote.setElectionId(Integer.MAX_VALUE);
    return vote;
  }

  /**
   * One case per concrete MailMessage implementation, each built with the longest value its
   * reference id source can hold. {@link #testAllMailMessageImplementationsAreCovered()} fails if a
   * new implementation is added without a case here.
   */
  private static Stream<MessageCase> messagesWithLongestPossibleReferenceIds() {
    return Stream.of(
        messageCase(
            "dar reference id",
            new DACMembersDARRADARApprovedMessage(
                user(), "DAR-1", user(), LONGEST_BOUNDED_SOURCE, DATASET_DTOS)),
        messageCase(
            "the digest date",
            new DacVoteDigestMessage(
                user(),
                List.of(new Reminder(1, "DAR-1", 1, Instant.parse("2026-07-30T00:00:00Z"))),
                LONGEST_BOUNDED_SOURCE,
                Instant.parse("2026-07-30T00:00:00Z"))),
        messageCase(
            "dar reference id",
            new DarExpirationReminderMessage(user(), "DAR-1", LONGEST_BOUNDED_SOURCE)),
        messageCase(
            "dar reference id", new DarExpiredMessage(user(), "DAR-1", LONGEST_BOUNDED_SOURCE)),
        messageCase(
            "dar code",
            new DataCustodianApprovalMessage(
                user(),
                LONGEST_BOUNDED_SOURCE,
                DATASET_DTOS,
                "Data Depositor",
                "researcher@user.com",
                false)),
        messageCase(
            "dataset identifier",
            new DatasetApprovedMessage(user(), "DAC", LONGEST_BOUNDED_SOURCE, "dataset name")),
        messageCase(
            // Fed an over-wide dataset name so the case also fails if the reference id goes
            // back to being keyed on the name rather than the identifier.
            "the dataset identifier, not the dataset name",
            new DatasetDeniedMessage(
                user(), "DAC", LONGEST_BOUNDED_SOURCE, UNBOUNDED_TEXT_SOURCE, "dac@email.com")),
        messageCase(
            "dataset name",
            new DatasetSubmittedMessage(
                user(), "Data Submitter", LONGEST_BOUNDED_SOURCE, "DAC name")),
        messageCase("dar code", new NewCaseMessage(user(), LONGEST_BOUNDED_SOURCE)),
        messageCase(
            "dac name",
            new NewDAAUploadResearcherMessage(
                user(), LONGEST_BOUNDED_SOURCE, "previous.pdf", "new.pdf")),
        messageCase(
            "dac name",
            new NewDAAUploadSOMessage(user(), LONGEST_BOUNDED_SOURCE, "previous.pdf", "new.pdf")),
        messageCase(
            "dar code",
            new NewDARRequestMessage(
                user(),
                LONGEST_BOUNDED_SOURCE,
                Map.of("DAC", List.of("DUOS-000001")),
                "Researcher")),
        messageCase(
            "dar code",
            new NewDARSigningOfficialRequestMessage(
                user(), LONGEST_BOUNDED_SOURCE, "Researcher Name")),
        messageCase(
            "the recipient's email", new NewLibraryCardIssuedMessage(userWithLongestEmail())),
        messageCase("dar code", new NewProgressReportCaseMessage(user(), LONGEST_BOUNDED_SOURCE)),
        messageCase(
            "dar reference id",
            new NewProgressReportRequestMessage(
                user(),
                "DAR-1",
                LONGEST_BOUNDED_SOURCE,
                Map.of("DAC", List.of("DUOS-000001")),
                "Researcher")),
        messageCase(
            "the digest date",
            new NewStudyDigestMessage(
                user(),
                List.of(new StudyDatasetCountRecord("study name", 1, "Controlled", 1)),
                LONGEST_BOUNDED_SOURCE)),
        messageCase(
            "the study uuid, not the unbounded study name",
            new NewStudyRegistrationConfirmationMessage(
                user(), UNBOUNDED_TEXT_SOURCE, 1, UUID.randomUUID(), Map.of())),
        messageCase(
            "the election id",
            new ReminderMessage(user(), voteWithLargestElectionId(), "DAR-1", "http://voteUrl")),
        messageCase(
            "dar code",
            new ResearcherApprovedProgressReportMessage(
                user(), LONGEST_BOUNDED_SOURCE, DATASET_DTOS, "data use restriction", false)),
        messageCase(
            "dar reference id",
            new ResearcherCloseoutCompletedMessage(user(), "DAR-1", LONGEST_BOUNDED_SOURCE)),
        messageCase(
            "dar code",
            new ResearcherDarApprovedMessage(
                user(), LONGEST_BOUNDED_SOURCE, DATASET_DTOS, "data use restriction", false)),
        messageCase(
            "dar reference id",
            new SoDARApproved(
                user(),
                "DAR-1",
                user(),
                LONGEST_BOUNDED_SOURCE,
                List.of(),
                "data use restriction",
                false)),
        messageCase(
            "dar reference id",
            new SoDARSubmitted(user(), "DAR-1", user(), LONGEST_BOUNDED_SOURCE, List.of())),
        messageCase(
            "dar reference id",
            new SoPRApproved(
                user(),
                "DAR-1",
                user(),
                LONGEST_BOUNDED_SOURCE,
                List.of(),
                "data use restriction",
                false)),
        messageCase(
            "dar reference id",
            new SoPRSubmitted(user(), "DAR-1", user(), LONGEST_BOUNDED_SOURCE, List.of())),
        messageCase(
            "dar reference id",
            new SubmittedCloseoutMessage(
                user(), "DAR-1", LONGEST_BOUNDED_SOURCE, "http://closeoutUrl")));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("messagesWithLongestPossibleReferenceIds")
  void testGetEntityReferenceId_FitsInColumn(MessageCase messageCase) {
    assertReferenceIdFitsInColumn(messageCase.message());
  }

  /**
   * A message keyed on an unbounded source is the defect being guarded against, and it must fail
   * the assertion above. Without this, a change that made the assertion vacuous would leave the
   * whole class silently passing.
   */
  @Test
  void testGetEntityReferenceId_UnboundedSourceIsRejected() {
    MailMessage keyedOnUnboundedText =
        new MailMessage(user(), EmailType.NEW_STUDY_REGISTRATION_CONFIRMATION) {
          @Override
          public String createSubject() {
            return "subject";
          }

          @Override
          public Map<String, Object> createModel() {
            return Map.of();
          }

          @Override
          public String getEntityReferenceId() {
            return UNBOUNDED_TEXT_SOURCE;
          }
        };

    AssertionError error =
        assertThrows(
            AssertionError.class, () -> assertReferenceIdFitsInColumn(keyedOnUnboundedText));
    assertTrue(error.getMessage().contains("1000 characters"), error.getMessage());
  }

  /**
   * Guards the case list above against drift: a new MailMessage implementation has to be given a
   * case here, which forces a decision about what its reference id is keyed on.
   */
  @Test
  void testAllMailMessageImplementationsAreCovered() throws IOException {
    Set<String> covered =
        messagesWithLongestPossibleReferenceIds()
            .map(messageCase -> messageCase.messageType().getSimpleName())
            .collect(Collectors.toCollection(TreeSet::new));

    Set<String> implementations =
        ClassPath.from(getClass().getClassLoader())
            .getTopLevelClasses(MailMessage.class.getPackageName())
            .stream()
            .map(ClassPath.ClassInfo::load)
            .filter(MailMessage.class::isAssignableFrom)
            .filter(candidate -> !Modifier.isAbstract(candidate.getModifiers()))
            .map(Class::getSimpleName)
            .collect(Collectors.toCollection(TreeSet::new));

    assertTrue(
        implementations.size() > 1,
        "Expected to discover the MailMessage implementations, found " + implementations);
    assertEquals(
        implementations,
        covered,
        "Every MailMessage implementation needs an entity reference id length case");
  }

  private static void assertReferenceIdFitsInColumn(MailMessage message) {
    String entityReferenceId = message.getEntityReferenceId();
    if (Objects.isNull(entityReferenceId)) {
      return;
    }
    assertTrue(
        entityReferenceId.length() <= ENTITY_REFERENCE_ID_COLUMN_WIDTH,
        "%s returned an entity reference id of %d characters, which does not fit in the %d character email_entity.entity_reference_id column"
            .formatted(
                message.getClass().getSimpleName(),
                entityReferenceId.length(),
                ENTITY_REFERENCE_ID_COLUMN_WIDTH));
  }
}
