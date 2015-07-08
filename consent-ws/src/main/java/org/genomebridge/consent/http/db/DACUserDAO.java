package org.genomebridge.consent.http.db;

import org.genomebridge.consent.http.models.Consent;
import org.genomebridge.consent.http.models.DACUser;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;


@RegisterMapper({ DACUserMapper.class })
public interface DACUserDAO extends Transactional<DACUserDAO> {


    @SqlQuery("select * from dacuser where dacUserId = :dacUserId")
    String findDACUserById(@Bind("dacUserId") Integer dacUserId);

    @SqlQuery("select * from dacuser where email = :email")
    DACUser findDACUserByEmail(@Bind("email") String email);

    @SqlUpdate("insert into dacuser " +
            "(email, displayName, memberStatus" +
            "( :email, :displayName, :memberStatus)")
    @GetGeneratedKeys
    Integer insertDACUser( @Bind("email") String email,
                            @Bind("displayName") String displayName,
                            @Bind("memberStatus") String memberStatus);

}



