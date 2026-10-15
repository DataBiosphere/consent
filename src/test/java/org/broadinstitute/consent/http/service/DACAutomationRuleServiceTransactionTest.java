package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.MoreExecutors;
import java.util.UUID;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.rules.DACAutomationRule;
import org.broadinstitute.consent.http.rules.DACAutomationRuleType;
import org.broadinstitute.consent.http.rules.GeneralResearchUseV1;
import org.broadinstitute.consent.http.rules.RuleState;
import org.broadinstitute.consent.http.service.dao.VoteServiceDAO;
import org.junit.jupiter.api.Test;

class DACAutomationRuleServiceTransactionTest extends DAOTestHelper {

  @Test
  void failedRadarVoteRollsBackTheElectionAndVote() {
    User admin = createUser();
    DataAccessRequest dar = createDataAccessRequestV3();
    Integer datasetId =
        datasetDAO.insertDataset(
            "Synthetic dataset " + UUID.randomUUID(),
            FIXED_TIMESTAMP,
            admin.getUserId(),
            UUID.randomUUID().toString(),
            EMPTY_JSON_DOCUMENT,
            null);
    Dataset dataset = datasetDAO.findDatasetById(datasetId);
    DACAutomationRule rule =
        new DACAutomationRule(
            1,
            DACAutomationRuleType.GRU_V1,
            "Synthetic rule",
            RuleState.AVAILABLE,
            FIXED_TIMESTAMP,
            admin.getUserId(),
            "admin",
            "admin@example.com");
    VoteServiceDAO failingVoteServiceDAO = mock(VoteServiceDAO.class);
    when(failingVoteServiceDAO.updateVotesWithValue(any(), anyBoolean(), anyString()))
        .thenThrow(new IllegalStateException("Synthetic vote failure"));
    DACAutomationRuleService service =
        new DACAutomationRuleService(
            jdbi,
            failingVoteServiceDAO,
            mock(VoteService.class),
            mock(ElasticSearchService.class),
            MoreExecutors.newDirectExecutorService());

    assertNull(
        service.openElectionAndApprove(rule, new GeneralResearchUseV1(), dar, dataset, null));

    // The election and vote were written before the vote update failed.
    verify(failingVoteServiceDAO).updateVotesWithValue(any(), anyBoolean(), anyString());
    assertTrue(electionDAO.findElectionsByReferenceId(dar.getReferenceId()).isEmpty());
    assertTrue(voteDAO.findVotesByUserId(admin.getUserId()).isEmpty());
  }
}
