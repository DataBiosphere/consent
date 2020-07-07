package org.broadinstitute.consent.http.db;

import java.util.List;
import org.broadinstitute.consent.http.db.mapper.DacMapper;
import org.broadinstitute.consent.http.db.mapper.DataAccessRequestMapper;
import org.broadinstitute.consent.http.db.mapper.DataSetMapper;
import org.broadinstitute.consent.http.db.mapper.MatchMapper;
import org.broadinstitute.consent.http.db.mapper.SimpleElectionMapper;
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
      "SELECT id, reference_id, draft, (data #>> '{}')::jsonb AS data FROM data_access_request "
          + " WHERE draft != true ")
  List<DataAccessRequest> findAllDars();

  @SqlQuery(
      " SELECT e.* FROM election e "
          + " INNER JOIN "
          + "   (SELECT e.referenceid, MAX(e.createdate) AS maxDate "
          + "    FROM election e "
          + "    GROUP BY e.referenceid ) electionView ON electionView.maxDate = e.createDate AND electionView.referenceId = e.referenceId "
          + " WHERE e.referenceid in (<referenceIds>) ")
  @UseRowMapper(SimpleElectionMapper.class)
  List<Election> findLastElectionsByReferenceIds(
      @BindList("referenceIds") List<String> referenceIds);

  @SqlQuery("SELECT * FROM match_entity WHERE purpose IN (<referenceIds>)")
  @UseRowMapper(MatchMapper.class)
  List<Match> findMatchesForReferenceIds(@BindList("referenceIds") List<String> referenceIds);

  @SqlQuery(
      "SELECT d.*, e.electionid as electionId "
          + "FROM dac d "
          + "INNER JOIN consents c on d.dac_id = c.dac_id "
          + "INNER JOIN consentassociations a on a.consentId = c.consentId "
          + "INNER JOIN election e on e.datasetId = a.dataSetId "
          + "WHERE e.electionid in (<electionIds>)"
          + "UNION "
          + "SELECT d.*, e.electionId as electionId "
          + "FROM dac d "
          + "INNER JOIN consents c on d.dac_id = c.dac_id "
          + "INNER JOIN election e on e.referenceId = c.consentId "
          + "WHERE e.electionid in (<electionIds>)")
  @UseRowMapper(DacMapper.class)
  List<Dac> findAllDacsForElectionIds(@BindList("electionIds") List<Integer> electionIds);

  @SqlQuery("SELECT * FROM dataset WHERE datasetid IN (<datasetIdList>)")
  @UseRowMapper(DataSetMapper.class)
  List<DataSet> findDatasetsByIdList(@BindList("datasetIdList") List<Integer> datasetIdList);
}
