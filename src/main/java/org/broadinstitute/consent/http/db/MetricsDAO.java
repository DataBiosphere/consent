package org.broadinstitute.consent.http.db;

import java.util.List;
import org.broadinstitute.consent.http.db.mapper.DacMapper;
import org.broadinstitute.consent.http.db.mapper.DataAccessRequestMapper;
import org.broadinstitute.consent.http.db.mapper.DataSetMapper;
import org.broadinstitute.consent.http.db.mapper.ElectionMapper;
import org.broadinstitute.consent.http.db.mapper.MatchMapper;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Match;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.transaction.Transactional;

public interface MetricsDAO extends Transactional<MetricsDAO> {

  @RegisterRowMapper(DataAccessRequestMapper.class)
  @SqlQuery(
      "SELECT id, reference_id, draft, user_id, create_date, sort_date, submission_date, update_date, (data #>> '{}')::jsonb AS data FROM data_access_request "
          + " WHERE draft != true ")
  List<DataAccessRequest> findAllDars();

  @SqlQuery(
    "SELECT * from ( "
      + "SELECT e.*, v.vote finalvote, "
      + "CASE "
        + "WHEN v.updatedate IS NULL THEN v.createdate "
        + "ELSE v.updatedate "
      + "END as finalvotedate, "
      + "v.rationale finalrationale, MAX(e.electionid) "
      + "OVER (PARTITION BY e.referenceid, e.electiontype) AS latest "
      + "FROM election e "
      + "LEFT JOIN vote v ON e.electionid = v.electionid AND "
      + "CASE "
        + "WHEN LOWER(e.electiontype) = 'dataaccess' THEN 'final'"
        + "WHEN LOWER(e.electiontype) = 'dataset' THEN 'data_owner' "
        + "ELSE 'chairperson' "
      + "END = LOWER(v.type) "
      + "WHERE e.referenceid in(<referenceIds>) "
    + ") AS results "
    + "WHERE results.latest = results.electionid "
    + "ORDER BY results.electionid DESC, "
    + "CASE "
      + "WHEN results.finalvotedate IS NULL THEN results.lastupdate "
      + "ELSE results.finalvotedate "
    + "END DESC"
  )
  @UseRowMapper(ElectionMapper.class)
  List<Election> findLastElectionsByReferenceIds(
      @BindList("referenceIds") List<String> referenceIds);

  @SqlQuery("SELECT * FROM match_entity WHERE purpose IN (<referenceIds>)")
  @UseRowMapper(MatchMapper.class)
  List<Match> findMatchesForPurposeIds(@BindList("referenceIds") List<String> referenceIds);

  @SqlQuery(
    "SELECT d.*, e.electionid as electionid "
    + "FROM election e "
    + "INNER JOIN accesselection_consentelection a ON a.access_election_id = e.electionid "
    + "INNER JOIN election consentElection ON a.consent_election_id = consentElection.electionid "
    + "INNER JOIN consents c ON consentElection.referenceId = c.consentid "
    + "INNER JOIN dac d on d.dac_id = c.dac_id "
    + "WHERE e.electionId IN (<electionIds>) "
  )
  @UseRowMapper(DacMapper.class)
  List<Dac> findAllDacsForElectionIds(@BindList("electionIds") List<Integer> electionIds);

  @SqlQuery("SELECT * FROM dataset WHERE datasetid IN (<datasetIds>)")
  @UseRowMapper(DataSetMapper.class)
  List<DataSet> findDatasetsByIds(@BindList("datasetIds") List<Integer> datasetIds);
}
