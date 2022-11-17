package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.FileStorageObjectMapper;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

import java.util.Date;
import java.util.List;

@RegisterRowMapper(FileStorageObjectMapper.class)
public interface FileStorageObjectDAO extends Transactional<InstitutionDAO> {

    @SqlUpdate(
        "INSERT INTO file_storage_object "
            + " (file_name, category, gcs_file_uri, "
            + " media_type, entity_id, create_user_id,"
            + " create_date, deleted) "
            + " VALUES "
            + " (:fileName, :category, :gcsFileUri, "
            + " :mediaType, :entityId, :createUserId, "
            + " :createDate, false) "
    )
    @GetGeneratedKeys
    Integer insertNewFile(
            @Bind("fileName") String fileName,
            @Bind("category") String category,
            @Bind("gcsFileUri") String gcsFileUri,
            @Bind("mediaType") String mediaType,
            @Bind("entityId") String entityId,
            @Bind("createUserId") Integer createUserId,
            @Bind("createDate") Date createDate
    );

    @SqlUpdate(
            "UPDATE file_storage_object SET deleted=true, delete_user_id=:deleteUserId, delete_date=:deleteDate WHERE file_storage_object_id = :fileStorageObjectId"
    )
    void deleteFileById(@Bind("fileStorageObjectId") Integer fileStorageObjectId,
                        @Bind("deleteUserId") Integer deleteUserId,
                        @Bind("deleteDate") Date deleteDate);

    @SqlUpdate(
            "UPDATE file_storage_object SET deleted=true, delete_user_id=:deleteUserId, delete_date=:deleteDate WHERE entity_id = :entityId"
    )
    void deleteFilesByEntityId(@Bind("entityId") String entityId,
                               @Bind("deleteUserId") Integer deleteUserId,
                               @Bind("deleteDate") Date deleteDate);


    @SqlQuery(
            "SELECT * FROM file_storage_object WHERE file_storage_object_id = :fileStorageObjectId"
    )
    FileStorageObject findFileById(@Bind("fileStorageObjectId") Integer fileStorageObjectId);

    @SqlQuery(
            "SELECT * FROM file_storage_object WHERE entity_id = :entityId AND deleted != true"
    )
    List<FileStorageObject> findFilesByEntityId(@Bind("entityId") String entityId);

    @SqlQuery(
            "SELECT * FROM file_storage_object WHERE entity_id = :entityId AND category = :category AND deleted != true"
    )
    List<FileStorageObject> findFilesByEntityIdAndCategory(@Bind("entityId") String entityId, @Bind("category") String category);
}
