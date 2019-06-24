package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.Dac;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;

import java.util.Date;
import java.util.List;

@UseStringTemplate3StatementLocator
@RegisterMapper({DACUserMapper.class})
public interface DacDAO extends Transactional<DacDAO> {

    @SqlQuery("select * from dac")
    List<Dac> findAll();

    @SqlQuery("select * from dac where dac_id = :dacId")
    Dac findById(@Bind("dacId") Integer dacId);

    @SqlUpdate("insert into dac (name, description, create_date) values (:name, :description, :createDate)")
    @GetGeneratedKeys
    Integer createDac(@Bind("name") String name, @Bind("description") String description, @Bind("createDate") Date createDate);

    @SqlUpdate("update dac set name = :name, description = :description, update_date = :updateDate) where dac_id = :dacId")
    void updateDac(
            @Bind("name") String name,
            @Bind("description") String description,
            @Bind("updateDate") Date updateDate,
            @Bind("dacId") Integer dacId);


    @SqlUpdate("delete from dac where dac_id = :dacId")
    void deleteDac(@Bind("dacId") Integer dacId);

}
