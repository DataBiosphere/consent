package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.CounterMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@RegisterRowMapper(CounterMapper.class)
public interface CounterDAO extends Transactional<CounterDAO> {

  @SqlUpdate("INSERT INTO counter (name, count) VALUES (:name, :count) ")
  void addCounter(@Bind("name") String name, @Bind("count") Integer count);

  @SqlQuery(
      " WITH m AS ( "
          + "    UPDATE counter SET count = subquery.max_count + 1 "
          + "    FROM (SELECT MAX(count) as max_count FROM counter WHERE name = :name) AS subquery "
          + "    WHERE name = :name "
          + "    RETURNING * "
          + " ) "
          + " SELECT MAX(count) FROM m WHERE name = :name ")
  Integer incrementCountByName(@Bind("name") String name);

}
