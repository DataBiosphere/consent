package org.broadinstitute.consent.http.service.dao;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Update;

import java.sql.SQLException;
import java.util.List;

public class NihServiceDAO implements ConsentLogger {

    private final Jdbi jdbi;

    @Inject
    public NihServiceDAO(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public void updateUserNihStatus(User user, NIHUserAccount nihAccount) throws SQLException {
        jdbi.useHandle(handle -> {
            handle.getConnection().setAutoCommit(false);
            List<Update> updates = List.of(
                createUpsertPropStatement(handle, user.getUserId(), UserFields.ERA_STATUS.getValue(), nihAccount.getStatus().toString()),
                createUpsertPropStatement(handle, user.getUserId(), UserFields.ERA_EXPIRATION_DATE.getValue(), nihAccount.getEraExpiration()),
                createUpdateLCStatement(handle, user.getUserId(), nihAccount.getNihUsername()),
                createUpdateUserStatement(handle, user.getUserId(), nihAccount.getNihUsername()));
            updates.forEach(update -> {
                try {
                    update.execute();
                } catch (Exception e) {
                    logException(e);
                }
            });
            handle.commit();
        });
    }

    private Update createUpsertPropStatement(Handle handle, Integer userId, String propertyKey, String propertyValue) {
        String sql = """
                    INSERT INTO user_property (userid, propertykey, propertyvalue)
                    VALUES (:userId, :propertyKey, :propertyValue)
                    ON CONFLICT (userid, propertykey)
                    DO UPDATE SET propertyvalue = :propertyValue;
                """;
        Update update = handle.createUpdate(sql);
        update.bind("userId", userId);
        update.bind("propertyKey", propertyKey);
        update.bind("propertyValue", propertyValue);
        return update;
    }

    private Update createUpdateLCStatement(Handle handle, Integer userId, String eraCommonsId) {
        String sql = """
                    UPDATE library_card
                    SET era_commons_id = :eraCommonsId
                    WHERE user_id = :userId
                """;
        Update update = handle.createUpdate(sql);
        update.bind("eraCommonsId", eraCommonsId);
        update.bind("userId", userId);
        return update;
    }

    private Update createUpdateUserStatement(Handle handle, Integer userId, String eraCommonsId) {
        String sql = """
                    UPDATE users
                    SET era_commons_id = :eraCommonsId
                    WHERE user_id = :userId;
                """;
        Update update = handle.createUpdate(sql);
        update.bind("eraCommonsId", eraCommonsId);
        update.bind("userId", userId);
        return update;
    }
}
