package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.reflect.ClassPath;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Reminder;
import org.broadinstitute.consent.http.models.StudyDatasetCountRecord;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Every email escapes the user-supplied text it renders, so it can't inject markup. */
class MailTemplateEscapingTest extends AbstractMailMessageTest {

  private static final String MARKUP = "<i id=\"injected\">x</i>";

  private static final List<DatasetMailDTO> DATASET_DTOS =
      List.of(new DatasetMailDTO(1, MARKUP, "DUOS-000001", "https://example.org/data"));

  private record MessageCase(MailMessage message) {
    @Override
    public String toString() {
      return message.getClass().getSimpleName();
    }
  }

  private static User user() {
    User user = new User();
    user.setUserId(1);
    user.setDisplayName(MARKUP);
    user.setEmail("test@user.com");
    return user;
  }

  private static Vote vote() {
    Vote vote = new Vote();
    vote.setVoteId(1);
    vote.setElectionId(1);
    return vote;
  }

  private static List<Dataset> datasets() {
    Dataset dataset = new Dataset();
    dataset.setName(MARKUP);
    dataset.setAlias(1);
    dataset.setDatasetIdentifier();
    return List.of(dataset);
  }

  private static Stream<MessageCase> messagesWithMarkupValues() {
    Instant digestDate = Instant.parse("2026-07-30T00:00:00Z");
    Map<String, List<String>> dacDatasets = Map.of(MARKUP, List.of(MARKUP));
    return Stream.of(
            new DACMembersDARRADARApprovedMessage(user(), MARKUP, user(), "ref", DATASET_DTOS),
            new DacVoteDigestMessage(
                user(), List.of(new Reminder(1, MARKUP, 1, digestDate)), "2026-07-30", digestDate),
            new DarExpirationReminderMessage(user(), MARKUP, "ref"),
            new DarExpiredMessage(user(), MARKUP, "ref"),
            new DataCustodianApprovalMessage(user(), MARKUP, DATASET_DTOS, MARKUP, MARKUP, false),
            new DatasetApprovedMessage(user(), MARKUP, "DUOS-000001", MARKUP),
            new DatasetDeniedMessage(user(), MARKUP, "DUOS-000001", MARKUP, "dac@email.com"),
            new DatasetSubmittedMessage(user(), MARKUP, MARKUP, MARKUP),
            new NewCaseMessage(user(), MARKUP, List.of(1)),
            new NewDAAUploadResearcherMessage(user(), MARKUP, MARKUP, MARKUP),
            new NewDAAUploadSOMessage(user(), MARKUP, MARKUP, MARKUP),
            new NewDARRequestMessage(user(), MARKUP, dacDatasets, MARKUP),
            new NewDARSigningOfficialRequestMessage(user(), MARKUP, MARKUP),
            new NewLibraryCardIssuedMessage(user()),
            new NewProgressReportCaseMessage(user(), MARKUP, List.of(1)),
            new NewProgressReportRequestMessage(user(), MARKUP, "ref", dacDatasets, MARKUP),
            new NewStudyDigestMessage(
                user(), List.of(new StudyDatasetCountRecord(MARKUP, 1, "Controlled", 1)), "date"),
            new NewStudyRegistrationConfirmationMessage(
                user(), MARKUP, 1, UUID.randomUUID(), Map.of()),
            new ReminderMessage(user(), vote(), MARKUP, "http://voteUrl"),
            new ResearcherApprovedProgressReportMessage(
                user(), MARKUP, DATASET_DTOS, MARKUP, false),
            new ResearcherCloseoutCompletedMessage(user(), MARKUP, "ref"),
            new ResearcherDarApprovedMessage(user(), MARKUP, DATASET_DTOS, MARKUP, false),
            new SoDARApproved(user(), MARKUP, user(), "ref", datasets(), MARKUP, false),
            new SoDARSubmitted(user(), MARKUP, user(), "ref", datasets()),
            new SoPRApproved(user(), MARKUP, user(), "ref", datasets(), MARKUP, false),
            new SoPRSubmitted(user(), MARKUP, user(), "ref", datasets()),
            new SubmittedCloseoutMessage(user(), MARKUP, "ref", "http://closeoutUrl"))
        .map(MessageCase::new);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("messagesWithMarkupValues")
  void testTemplateEscapesUserSuppliedText(MessageCase messageCase) throws Exception {
    var rendered = renderTemplate(messageCase.message(), "https://duos.example.org/");

    assertTrue(rendered.document().select("#injected").isEmpty(), "markup rendered unescaped");
    assertTrue(
        rendered.content().contains("&lt;i id=&quot;injected&quot;&gt;"),
        "expected the escaped markup to be rendered as text");
  }

  @Test
  void testAllMailMessageImplementationsAreCovered() throws IOException {
    Set<String> covered =
        messagesWithMarkupValues()
            .map(messageCase -> messageCase.message().getClass().getSimpleName())
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

    assertEquals(implementations, covered, "Every MailMessage implementation needs a case here");
  }
}
