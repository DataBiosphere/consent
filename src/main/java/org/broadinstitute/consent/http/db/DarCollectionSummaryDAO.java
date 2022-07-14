package org.broadinstitute.consent.http.db;

import java.util.List;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.DarCollectionSummary;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.db.mapper.DarCollectionSummaryReducer;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.sqlobject.transaction.Transactional;

public interface DarCollectionSummaryDAO extends Transactional<DarCollectionSummaryDAO> {

  @RegisterBeanMapper(value = DarCollectionSummary.class)
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = Vote.class, prefix = "v")
  @RegisterBeanMapper(value = Election.class)
  @UseRowReducer(DarCollectionSummaryReducer.class)
  @SqlQuery(
    "SELECT c.id as dar_collection_id, c.dar_code, dar.submission_date, u.display_name as researcher_name, " +
      "i.institution_name , COUNT(DISTINCT(d.datasetid)) as dataset_count, e.*, v.voteid as v_vote_id " +
      "v.dacuserid as v_dac_user_id, v.vote as v_vote, v.electionid as v_election_id, v.createdate as v_create_date, v.updatedate as v_update_date, v.type as v_type " +
      "dar.data as dar_data " +
    "FROM dar_collection c " +
    "INNER JOIN users u " +
      "ON u.user_id = c.create_user_id " +
    "INNER JOIN institution i " +
      "ON institution.institution_id = u.institution_id " +
    "INNER JOIN data_access_request dar " +
      "ON dar.collection_id = c.collection_id " +
    "LEFT JOIN (" +
      "SELECT election.*, MAX(election.electionid) OVER(PARTITION BY election.referenceid, election.datasetid) AS latest " +
      "FROM election" +
      "WHERE LOWER(election.electiontype) = 'dataaccess'" +
    ") as e " +
      "ON e.referenceid = dar.reference_id " +
      "WHERE e.latest = e.electionid " +
    "LEFT JOIN vote v " +
      "ON e.electionid = v.electionid " +
      "WHERE LOWER(v.type) = 'final' OR v.dacuserid = :currentUserId " +
    "INNER JOIN dataset d " +
      "ON e.datasetid = d.datasetid " +
      "WHERE d.datasetid in <datasetIds>")
  List<DarCollectionSummary> getDarCollectionSummariesForDAC(
      @Bind("currentUserId") Integer currentUserId,
      @BindList("datasetIds") List<Integer> datasetIds);
}
