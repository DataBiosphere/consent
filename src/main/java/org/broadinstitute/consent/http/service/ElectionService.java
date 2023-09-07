package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.models.Election;

public class ElectionService {

  private final ElectionDAO electionDAO;

  @Inject
  public ElectionService(ElectionDAO electionDAO) {
    this.electionDAO = electionDAO;
  }

  public List<Election> findElectionsWithCardHoldingUsersByElectionIds(List<Integer> electionIds) {
    return !electionIds.isEmpty() ? electionDAO.findElectionsWithCardHoldingUsersByElectionIds(
        electionIds) : Collections.emptyList();
  }

  public List<Election> findElectionsByVoteIdsAndType(List<Integer> voteIds, String electionType) {
    return !voteIds.isEmpty() ? electionDAO.findElectionsByVoteIdsAndType(voteIds, electionType)
        : Collections.emptyList();
  }

}
