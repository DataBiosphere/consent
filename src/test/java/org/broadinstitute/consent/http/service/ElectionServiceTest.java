package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.models.Election;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ElectionServiceTest {

  private ElectionService service;

  @Mock private Jdbi jdbi;
  @Mock private ElectionDAO electionDAO;

  @BeforeEach
  void setUp() {
    when(jdbi.onDemand(ElectionDAO.class)).thenReturn(electionDAO);
    service = new ElectionService(jdbi);
  }

  @Test
  void findElectionsByVoteIdsAndType() {
    Election election = new Election();
    when(electionDAO.findElectionsByVoteIdsAndType(anyList(), anyString()))
        .thenReturn(List.of(election));
    List<Election> elections = service.findElectionsByVoteIdsAndType(List.of(1, 2), "test");
    assertNotNull(elections);
    assertEquals(1, elections.size());
  }

  @Test
  void findElectionsWithCardHoldingUsersByElectionIds() {
    Election election = new Election();
    when(electionDAO.findElectionsWithCardHoldingUsersByElectionIds(anyList()))
        .thenReturn(List.of(election));
    List<Election> elections = service.findElectionsWithCardHoldingUsersByElectionIds(List.of(1));
    assertNotNull(elections);
    assertEquals(1, elections.size());
  }
}
