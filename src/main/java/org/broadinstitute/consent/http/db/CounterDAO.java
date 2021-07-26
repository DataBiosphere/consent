package org.broadinstitute.consent.http.db;

import java.util.List;
import org.broadinstitute.consent.http.db.mapper.CounterMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@SuppressWarnings({"SqlResolve", "SqlNoDataSourceInspection", "SqlWithoutWhere"})
@RegisterRowMapper(CounterMapper.class)
public interface CounterDAO extends Transactional<CounterDAO> {

    @SqlUpdate("INSERT INTO counter (name, count) VALUES (:name, :count) ")
    void addCounter(@Bind("name") String name, @Bind("count") Integer count);

    @SqlQuery("SELECT MAX(count) FROM counter c WHERE name = :name ")
    Integer getMaxCountByName(@Bind("name") String name);

    @SqlUpdate("UPDATE counter " +
            "   SET count = subquery.max_count + 1 " +
            "   FROM (SELECT MAX(count) as max_count FROM counter WHERE name = :name) AS subquery " +
            "   WHERE name = :name")
    void incrementCountByName(@Bind("name") String name);

    @SqlUpdate("DELETE FROM counter")
    void deleteAll();

}
