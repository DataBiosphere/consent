package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

import java.util.Map;
import java.util.Objects;

public class DarCollectionReducer implements LinkedHashMapRowReducer<Integer, DarCollection>, RowMapperHelper {

    @Override
    public void accumulate(Map<Integer, DarCollection> map, RowView rowView) {
      DataAccessRequest dar = null;
      Election election = null;
      Vote vote = null;
      DarCollection collection = map.computeIfAbsent(
        rowView.getColumn("collection_id", Integer.class),
        id -> rowView.getRow(DarCollection.class));
      try {
        if(Objects.nonNull(collection)) {
          if(Objects.nonNull(rowView.getColumn("dar_id", Integer.class))) {
            dar = rowView.getRow(DataAccessRequest.class);
            DataAccessRequestData data = translate(rowView.getColumn("data", String.class));
            dar.setData(data);
          }
          if(Objects.nonNull(rowView.getColumn("e_election_id", Integer.class))) {
            election = rowView.getRow(Election.class);
          }
          if (Objects.nonNull(rowView.getColumn("v_vote_id", Integer.class))) {
            vote = rowView.getRow(Vote.class);
          }
        }
      } catch(MappingException e) {
        //ignore any exceptions
      }

      if(Objects.nonNull(dar)) {
        collection.addDar(dar);
      }
      if(Objects.nonNull(election)) {
        collection.addElection(election);
      }
      collection.addVote(vote);
    }
}
