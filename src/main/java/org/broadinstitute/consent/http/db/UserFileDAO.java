package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.UserFileMapper;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.UserFile;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

import java.util.Date;
import java.util.List;

@RegisterRowMapper(UserFileMapper.class)
public interface UserFileDAO extends Transactional<InstitutionDAO> {

    @SqlUpdate(
        "INSERT INTO user_file "
            + " (file_name, category, bucket_name, "
            + " blob_name, media_type, entity_id, create_user_id,"
            + " create_date, deleted) "
            + " VALUES "
            + " (:fileName, :category, :bucketName, "
            + " :blobName, :mediaType, :entityId, :createUserId, "
            + " :createDate, false) "
    )
    @GetGeneratedKeys
    Integer insertNewFile(
            @Bind("fileName") String fileName,
            @Bind("category") String category,
            @Bind("bucketName") String bucketName,
            @Bind("blobName") String blobName,
            @Bind("mediaType") String mediaType,
            @Bind("entityId") String entityId,
            @Bind("createUserId") Integer createUserId,
            @Bind("createDate") Date createDate
    );

    @SqlUpdate(
            "UPDATE user_file SET deleted=true WHERE user_file_id = :userFileId"
    )
    void deleteFileById(@Bind("userFileId") Integer userFileId);

    @SqlUpdate(
            "UPDATE user_file SET deleted=true WHERE entity_id = :entityId"
    )
    void deleteFilesByEntityId(@Bind("entityId") String entityId);


    @SqlQuery(
            "SELECT * FROM user_file WHERE user_file_id = :userFileId"
    )
    UserFile findFileById(@Bind("userFileId") Integer userFileId);

    @SqlQuery(
            "SELECT * FROM user_file WHERE entity_id = :entityId AND deleted != true"
    )
    List<UserFile> findFilesByEntityId(@Bind("entityId") String entityId);

    @SqlQuery(
            "SELECT * FROM user_file WHERE entity_id = :entityId AND category = :category AND deleted != true"
    )
    List<UserFile> findFilesByEntityIdAndCategory(@Bind("entityId") String entityId, @Bind("category") String category);
}
