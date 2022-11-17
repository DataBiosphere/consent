package org.broadinstitute.consent.http.db.mapper;


import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.cloud.storage.BlobId;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class FileStorageObjectMapper implements RowMapper<FileStorageObject>, RowMapperHelper {
    @Override
    public FileStorageObject map(ResultSet r, StatementContext statementContext) throws SQLException {
        FileStorageObject file = new FileStorageObject();
        if (hasColumn(r, "file_name")) {
            file.setFileName(r.getString("file_name"));
        }

        if (hasColumn(r, "file_storage_object_id")) {
            file.setFileStorageObjectId(r.getInt("file_storage_object_id"));
        }

        if (hasColumn(r, "entity_id")) {
            file.setEntityId(r.getString("entity_id"));
        }

        if (hasColumn(r, "gcs_file_uri")) {
            try {
                file.setBlobId(BlobId.fromGsUtilUri(r.getString("gcs_file_uri")));
            } catch (Exception e) {
                file.setBlobId(null);
            }
        }

        if (hasColumn(r, "category")) {
            try {
                file.setCategory(FileCategory.findValue(r.getString("category")));
            } catch(Exception e) {
                file.setCategory(null);
            }
        }

        if (hasColumn(r, "media_type")) {
            file.setMediaType(r.getString("media_type"));
        }

        if (hasColumn(r, "create_user_id")) {
            file.setCreateUserId(r.getInt("create_user_id"));
        }

        if (hasColumn(r, "create_date")) {
            file.setCreateDate(r.getDate("create_date"));
        }

        if (hasColumn(r, "deleted")) {
            file.setDeleted(r.getBoolean("deleted"));
        }

        if (hasColumn(r, "delete_user_id")) {
            file.setDeleteUserId(r.getObject("delete_user_id", Integer.class));
        }

        if (hasColumn(r, "delete_date")) {
            file.setDeleteDate(r.getDate("delete_date"));
        }

        if (hasColumn(r, "update_user_id")) {
            file.setUpdateUserId(r.getObject("update_user_id", Integer.class));

        }

        if (hasColumn(r, "update_date")) {
            file.setUpdateDate(r.getDate("update_date"));
        }

        return file;
    }
}
