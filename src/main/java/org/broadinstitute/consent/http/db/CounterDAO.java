package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.CounterMapper;
import org.broadinstitute.consent.http.models.Counter;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@SuppressWarnings({"SqlResolve", "SqlNoDataSourceInspection", "SqlWithoutWhere"})
@RegisterRowMapper(CounterMapper.class)
public interface CounterDAO extends Transactional<CounterDAO> {

    @SqlQuery("SELECT MAX(count) FROM counter c WHERE name = :name")
    Integer getLastCountByName(@Bind("name") String name);

    @SqlUpdate("INSERT INTO counter (name) VALUES (:name)")
    @GetGeneratedKeys
    Integer incrementCounter(@Bind("name") String name);

    @SqlQuery("SELECT * FROM counter WHERE id = :id ")
    Counter getCounterById(@Bind("id") Integer id);

    @SqlUpdate("DELETE FROM counter")
    void deleteAll();

}
