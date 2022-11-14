package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.AcknowledgementMapper;
import org.broadinstitute.consent.http.models.Acknowledgement;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

import java.util.List;

@RegisterRowMapper(AcknowledgementMapper.class)
public interface AcknowledgementDAO extends Transactional<AcknowledgementDAO> {

    @SqlUpdate("INSERT INTO acknowledgement (ack_key, user_id, first_acknowledged, last_acknowledged) "
                    + " VALUES (:key, :userId, current_timestamp, current_timestamp) "
                    + " ON CONFLICT (ack_key, user_id) DO UPDATE SET last_acknowledged = current_timestamp ")
    void upsertAcknowledgement(@Bind("key") String key, @Bind("userId") Integer userId);

    @SqlQuery("SELECT ack_key, user_id, first_acknowledged, last_acknowledged "
            + " FROM acknowledgement WHERE ack_key = :key and user_id = :userId")
    Acknowledgement getAcknowledgementsByKeyForUser(@Bind("key") String key, @Bind("userId") Integer userId);

    @SqlQuery("SELECT ack_key, user_id, first_acknowledged, last_acknowledged "
            + " FROM acknowledgement WHERE user_id = :userId")
    List<Acknowledgement> getAcknowledgementsForUser(@Bind("userId") Integer userId);

    @SqlQuery("SELECT ack_key, user_id, first_acknowledged, last_acknowledged "
          + " FROM acknowledgement WHERE user_id = :userId and ack_key IN (<key_list>)")
    List<Acknowledgement> getAcknowledgementsForUser(@BindList("key_list") List<String> keys, @Bind("userId") Integer userId);

}
