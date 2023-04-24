package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.AcknowledgmentMapper;
import org.broadinstitute.consent.http.models.Acknowledgment;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

import java.util.List;

@RegisterRowMapper(AcknowledgmentMapper.class)
public interface AcknowledgmentDAO extends Transactional<AcknowledgmentDAO> {

    @SqlUpdate("INSERT INTO acknowledgment (ack_key, user_id, first_acknowledged, last_acknowledged) "
                    + " VALUES (:key, :userId, current_timestamp, current_timestamp) "
                    + " ON CONFLICT (ack_key, user_id) DO UPDATE SET last_acknowledged = current_timestamp ")
    void upsertAcknowledgment(@Bind("key") String key, @Bind("userId") Integer userId);

    @SqlQuery("SELECT ack_key, user_id, first_acknowledged, last_acknowledged "
            + " FROM acknowledgment WHERE ack_key = :key and user_id = :userId")
    Acknowledgment findAcknowledgmentsByKeyForUser(@Bind("key") String key, @Bind("userId") Integer userId);

    @SqlQuery("SELECT ack_key, user_id, first_acknowledged, last_acknowledged "
            + " FROM acknowledgment WHERE user_id = :userId")
    List<Acknowledgment> findAcknowledgmentsForUser(@Bind("userId") Integer userId);

    @SqlQuery("SELECT ack_key, user_id, first_acknowledged, last_acknowledged "
          + " FROM acknowledgment WHERE user_id = :userId and ack_key IN (<key_list>)")
    List<Acknowledgment> findAcknowledgmentsForUser(@BindList("key_list") List<String> keys, @Bind("userId") Integer userId);

    @SqlUpdate("DELETE FROM acknowledgment where user_id = :userId AND ack_key = :key")
    void deleteAcknowledgment(@Bind("key") String key, @Bind("userId") Integer userId);
}
