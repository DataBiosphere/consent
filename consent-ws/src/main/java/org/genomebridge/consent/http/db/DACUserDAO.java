package org.genomebridge.consent.http.db;


import org.genomebridge.consent.http.models.DACUser;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;

import java.util.List;


@RegisterMapper({DACUserMapper.class})
public interface DACUserDAO extends Transactional<DACUserDAO> {


    @SqlQuery("select * from dacuser where dacUserId = :dacUserId")
    DACUser findDACUserById(@Bind("dacUserId") Integer dacUserId);

    @SqlQuery("select * from dacuser where dacUserId = :dacUserId and memberStatus = 'CHAIRPERSON'")
    Integer checkChairpersonUser(@Bind("dacUserId") Integer dacUserId);

    @SqlQuery("select * from dacuser ")
    List<DACUser> findDACUsers();

    @SqlQuery("select * from dacuser where email = :email")
    DACUser findDACUserByEmail(@Bind("email") String email);

    @SqlUpdate("insert into dacuser " +
            "(email, displayName, memberStatus) values" +
            "( :email, :displayName, :memberStatus)")
    @GetGeneratedKeys
    Integer insertDACUser(@Bind("email") String email,
                          @Bind("displayName") String displayName,
                          @Bind("memberStatus") String memberStatus);

    @SqlUpdate("update dacuser set email=:email, " +
            "displayName=:displayName, memberStatus=:memberStatus " +
            "where email=:email")
    @GetGeneratedKeys
    Integer updateDACUser(@Bind("email") String email,
                          @Bind("displayName") String displayName,
                          @Bind("memberStatus") String memberStatus);

    @SqlUpdate("delete  from dacuser where email = :email")
    void deleteDACUserByEmail(@Bind("email") String email);
}



