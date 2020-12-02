package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.broadinstitute.consent.http.enumeration.ElectionFields;
import org.broadinstitute.consent.http.models.Election;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class ElectionMapper implements RowMapper<Election>, RowMapperHelper {

    private final Map<Integer, Election> electionMap = new HashMap<>();

    @Override
    public Election map(ResultSet r, StatementContext ctx) throws SQLException {
        Election election;
        if (electionMap.containsKey(r.getInt(ElectionFields.ID.getValue()))) {
            election = electionMap.get(r.getInt(ElectionFields.ID.getValue()));
        } else {
            election = new Election();
            election.setElectionId(r.getInt(ElectionFields.ID.getValue()));
        }
        if (r.getString(ElectionFields.TYPE.getValue()) != null) {
            election.setElectionType(r.getString(ElectionFields.TYPE.getValue()));
        }
        if (r.getString(ElectionFields.FINAL_VOTE.getValue()) != null) {
            election.setFinalVote(r.getBoolean(ElectionFields.FINAL_VOTE.getValue()));
        }
        if (r.getString(ElectionFields.FINAL_RATIONALE.getValue()) != null) {
            election.setFinalRationale(r.getString(ElectionFields.FINAL_RATIONALE.getValue()));
        }
        if (r.getString(ElectionFields.STATUS.getValue()) != null) {
            election.setStatus(r.getString(ElectionFields.STATUS.getValue()));
        }
        if (r.getDate(ElectionFields.CREATE_DATE.getValue()) != null) {
            election.setCreateDate(r.getDate(ElectionFields.CREATE_DATE.getValue()));
        }
        if (r.getDate(ElectionFields.FINAL_VOTE_DATE.getValue()) != null) {
            election.setFinalVoteDate(r.getDate(ElectionFields.FINAL_VOTE_DATE.getValue()));
        }
        if (r.getString(ElectionFields.REFERENCE_ID.getValue()) != null) {
            election.setReferenceId(r.getString(ElectionFields.REFERENCE_ID.getValue()));
        }
        if (r.getDate(ElectionFields.LAST_UPDATE.getValue()) != null) {
            election.setLastUpdate(r.getDate(ElectionFields.LAST_UPDATE.getValue()));
        }
        if (r.getString(ElectionFields.FINAL_ACCESS_VOTE.getValue()) != null) {
            election.setFinalAccessVote(r.getBoolean(ElectionFields.FINAL_ACCESS_VOTE.getValue()));
        }
        if (r.getObject(ElectionFields.DATASET_ID.getValue()) != null) {
            election.setDataSetId(r.getInt(ElectionFields.DATASET_ID.getValue()));
        }
        if (r.getString(ElectionFields.DATA_USE_LETTER.getValue()) != null) {
            election.setDataUseLetter(r.getString(ElectionFields.DATA_USE_LETTER.getValue()));
        }
        if (r.getString(ElectionFields.DUL_NAME.getValue()) != null) {
            election.setDulName(r.getString(ElectionFields.DUL_NAME.getValue()));
        }
        if (r.getObject(ElectionFields.VERSION.getValue()) != null) {
            election.setVersion(r.getInt(ElectionFields.VERSION.getValue()));
        }
        if (r.getString(ElectionFields.ARCHIVED.getValue()) != null) {
            election.setArchived(r.getBoolean(ElectionFields.ARCHIVED.getValue()));
        }
        electionMap.put(election.getElectionId(), election);
        return election;
    }
}
