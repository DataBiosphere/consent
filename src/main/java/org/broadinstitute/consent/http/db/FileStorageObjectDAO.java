package org.broadinstitute.consent.http.db;

import java.time.Instant;
import java.util.List;
import org.broadinstitute.consent.http.db.mapper.FileStorageObjectMapper;
import org.broadinstitute.consent.http.db.mapper.FileStorageObjectMapperWithFSOPrefix;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@RegisterRowMapper(FileStorageObjectMapper.class)
public interface FileStorageObjectDAO extends Transactional<InstitutionDAO> {

  @SqlUpdate(
      """
          INSERT INTO file_storage_object
          (file_name, category, gcs_file_uri,
          media_type, entity_id, create_user_id,
          create_date, deleted)
          VALUES
          (:fileName, :category, :gcsFileUri,
          :mediaType, :entityId, :createUserId,
          :createDate, false)
          """)
  @GetGeneratedKeys
  Integer insertNewFile(
      @Bind("fileName") String fileName,
      @Bind("category") String category,
      @Bind("gcsFileUri") String gcsFileUri,
      @Bind("mediaType") String mediaType,
      @Bind("entityId") String entityId,
      @Bind("createUserId") Integer createUserId,
      @Bind("createDate") Instant createDate);

  @SqlUpdate(
      """
          UPDATE file_storage_object
          SET deleted=true,
              delete_user_id=:deleteUserId,
              delete_date=NOW()
          WHERE file_storage_object_id = :fileStorageObjectId
          """)
  void deleteFileById(
      @Bind("fileStorageObjectId") Integer fileStorageObjectId,
      @Bind("deleteUserId") Integer deleteUserId);

  @SqlUpdate(
      """
          UPDATE file_storage_object
          SET gcs_file_uri=:gcsFileUri,
              media_type=:mediaType,
              update_user_id=:updateUserId,
              update_date=:updateDate
          WHERE file_storage_object_id = :fileStorageObjectId
          """)
  void updateFileById(
      @Bind("fileStorageObjectId") Integer fileStorageObjectId,
      @Bind("gcsFileUri") String gcsFileUri,
      @Bind("mediaType") String mediaType,
      @Bind("updateUserId") Integer updateUserId,
      @Bind("updateDate") Instant updateDate);

  @SqlUpdate(
      """
          UPDATE file_storage_object
          SET deleted=true,
              delete_user_id=:deleteUserId,
              delete_date=:deleteDate
          WHERE entity_id = :entityId
          """)
  void deleteFilesByEntityId(
      @Bind("entityId") String entityId,
      @Bind("deleteUserId") Integer deleteUserId,
      @Bind("deleteDate") Instant deleteDate);

  @SqlQuery(
      """
          SELECT *
          FROM file_storage_object
          WHERE file_storage_object_id = :fileStorageObjectId
          """)
  FileStorageObject findFileById(@Bind("fileStorageObjectId") Integer fileStorageObjectId);

  @SqlQuery(
      """
          SELECT *
          FROM file_storage_object
          WHERE entity_id = :entityId AND
                deleted != true
          """)
  List<FileStorageObject> findFilesByEntityId(@Bind("entityId") String entityId);

  @SqlQuery(
      """
          SELECT *
          FROM file_storage_object
          WHERE entity_id = :entityId
                AND category = :category
                AND deleted != true
          """)
  List<FileStorageObject> findFilesByEntityIdAndCategory(
      @Bind("entityId") String entityId, @Bind("category") String category);

  @UseRowMapper(FileStorageObjectMapperWithFSOPrefix.class)
  @SqlQuery(
      """
          SELECT
              fso.file_storage_object_id AS fso_file_storage_object_id,
              fso.entity_id AS fso_entity_id,
              fso.file_name AS fso_file_name,
              fso.category AS fso_category,
              fso.gcs_file_uri AS fso_gcs_file_uri,
              fso.media_type AS fso_media_type,
              fso.create_date AS fso_create_date,
              fso.create_user_id AS fso_create_user_id,
              fso.update_date AS fso_update_date,
              fso.update_user_id AS fso_update_user_id,
              fso.deleted AS fso_deleted,
              fso.delete_user_id AS fso_delete_user_id
          FROM file_storage_object fso
          WHERE fso.entity_id = :entityId
            AND (fso.deleted = false OR fso.deleted IS NULL)
          ORDER BY fso.create_date DESC
          """)
  List<FileStorageObject> findFileMetadataByEntityId(@Bind("entityId") String entityId);
}
