package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

import java.util.Map;
import java.util.Objects;

public class DarCollectionReducer
    implements LinkedHashMapRowReducer<Integer, DarCollection>, RowMapperHelper {

  @Override
  public void accumulate(Map<Integer, DarCollection> map, RowView rowView) {
    DataAccessRequest dar = null;
    Election election = null;
    Vote vote = null;
    User user = null;
    Institution institution = null;
    DarCollection collection =
        map.computeIfAbsent(
            rowView.getColumn("collection_id", Integer.class),
            id -> rowView.getRow(DarCollection.class));
    try {
      if (Objects.nonNull(collection)) {
        if (Objects.nonNull(rowView.getColumn("dar_id", Integer.class))) {
          dar = rowView.getRow(DataAccessRequest.class);
          String referenceId = dar.getReferenceId();
          DataAccessRequest savedDar = collection.getDars().get(referenceId);
          if (Objects.isNull(savedDar)) {
            DataAccessRequestData data = translate(rowView.getColumn("data", String.class));
            dar.setData(data);
          } else {
            dar = savedDar;
          }
        }
        if (Objects.nonNull(rowView.getColumn("e_election_id", Integer.class))) {
          election = rowView.getRow(Election.class);
          Integer electionId = election.getElectionId();
          Election savedElection = dar.getElections().get(electionId);
          if (Objects.nonNull(savedElection)) {
            election = savedElection;
          }
        }
        if (Objects.nonNull(rowView.getColumn("v_vote_id", Integer.class))) {
          vote = rowView.getRow(Vote.class);
        }
        if (Objects.nonNull(rowView.getColumn("u_dacuserid", Integer.class))) {
          user = rowView.getRow(User.class);
        }
        if (Objects.nonNull(rowView.getColumn("i_id", Integer.class))) {
          institution = rowView.getRow(Institution.class);
        }
      }
    } catch (MappingException e) {
      // ignore any exceptions
    }
    if (Objects.nonNull(vote)) {
      election.addVote(vote);
    }

    if (Objects.nonNull(election)) {
      dar.addElection(election);
    }

    if (Objects.nonNull(dar)) {
      collection.addDar(dar);
    }

    if (Objects.nonNull(user)) {
      if (Objects.nonNull(institution)) {
        user.setInstitution(institution);
      }
      collection.setCreateUser(user);
    }
  }
}
