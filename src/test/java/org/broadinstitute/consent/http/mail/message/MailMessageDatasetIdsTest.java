package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class MailMessageDatasetIdsTest {

  private static final User USER = new User();
  private static final List<DatasetMailDTO> DTOS = List.of(dto(7), dto(8));
  private static final List<Dataset> DATASETS = List.of(dataset(7), dataset(8));

  static Stream<MailMessage> messagesWithDatasets() {
    return Stream.of(
        new DataCustodianApprovalMessage(USER, "DAR-1", DTOS, "depositor", "r@x.org", false),
        new ResearcherDarApprovedMessage(USER, "DAR-1", DTOS, "restriction", false),
        new ResearcherApprovedProgressReportMessage(USER, "DAR-1", DTOS, "restriction", false),
        new DACMembersDARRADARApprovedMessage(USER, "DAR-1", USER, "ref", DTOS),
        new SoDARApproved(USER, "DAR-1", USER, "ref", DATASETS, "restriction", false),
        new SoPRApproved(USER, "DAR-1", USER, "ref", DATASETS, "restriction", false),
        new SoDARSubmitted(USER, "DAR-1", USER, "ref", DATASETS),
        new SoPRSubmitted(USER, "DAR-1", USER, "ref", DATASETS),
        new NewCaseMessage(USER, "DAR-1", List.of(7, 8)),
        new NewProgressReportCaseMessage(USER, "DAR-1", List.of(7, 8)));
  }

  @ParameterizedTest
  @MethodSource("messagesWithDatasets")
  void records_the_datasets_it_concerns(MailMessage message) {
    assertEquals(Optional.of(List.of(7, 8)), message.getDatasetIds());
  }

  @Test
  void records_no_datasets_by_default() {
    assertTrue(new DarExpiredMessage(USER, "DAR-1", "ref").getDatasetIds().isEmpty());
  }

  private static DatasetMailDTO dto(int id) {
    return new DatasetMailDTO(id, "dataset " + id, "DUOS-00000" + id, null);
  }

  private static Dataset dataset(int id) {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(id);
    return dataset;
  }
}
