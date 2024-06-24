package org.broadinstitute.consent.http.db.mapper;


import com.google.cloud.storage.BlobId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class FileStorageObjectMapper implements RowMapper<FileStorageObject>, RowMapperHelper {

  @Override
  public FileStorageObject map(ResultSet r, StatementContext statementContext) throws SQLException {
    FileStorageObject file = new FileStorageObject();

    if (hasColumn(r, addPrefix("file_name"))) {
      file.setFileName(r.getString(addPrefix("file_name")));
    }

    if (hasColumn(r, addPrefix("file_storage_object_id"))) {
      file.setFileStorageObjectId(r.getInt(addPrefix("file_storage_object_id")));
    }

    if (hasColumn(r, addPrefix("entity_id"))) {
      file.setEntityId(r.getString(addPrefix("entity_id")));
    }

    if (hasColumn(r, addPrefix("gcs_file_uri"))) {
      try {
        file.setBlobId(BlobId.fromGsUtilUri(r.getString(addPrefix("gcs_file_uri"))));
      } catch (Exception e) {
        file.setBlobId(null);
      }
    }

    if (hasColumn(r, addPrefix("category"))) {
      try {
        file.setCategory(FileCategory.findValue(r.getString(addPrefix("category"))));
      } catch (Exception e) {
        file.setCategory(null);
      }
    }

    if (hasColumn(r, addPrefix("media_type"))) {
      file.setMediaType(r.getString(addPrefix("media_type")));
    }

    if (hasNonZeroColumn(r, addPrefix("create_user_id"))) {
      file.setCreateUserId(r.getInt(addPrefix("create_user_id")));
    }

    if (hasColumn(r, addPrefix("create_date"))) {
      Timestamp createDate = r.getTimestamp(addPrefix("create_date"));
      file.setCreateDate((Objects.nonNull(createDate) ? createDate.toInstant() : null));
    }

    if (hasColumn(r, addPrefix("deleted"))) {
      file.setDeleted(r.getBoolean(addPrefix("deleted")));
    }

    if (hasNonZeroColumn(r, addPrefix("delete_user_id"))) {
      file.setDeleteUserId(r.getInt(addPrefix("delete_user_id")));
    }

    if (hasColumn(r, addPrefix("delete_date"))) {
      Timestamp deleteDate = r.getTimestamp(addPrefix("delete_date"));
      file.setDeleteDate((Objects.nonNull(deleteDate) ? deleteDate.toInstant() : null));
    }

    if (hasNonZeroColumn(r, addPrefix("update_user_id"))) {
      file.setUpdateUserId(r.getInt(addPrefix("update_user_id")));
    }

    if (hasColumn(r, addPrefix("update_date"))) {
      Timestamp updateDate = r.getTimestamp(addPrefix("update_date"));
      file.setUpdateDate((Objects.nonNull(updateDate) ? updateDate.toInstant() : null));
    }

    return file;
  }

  public String getPrefix() {
    return "";
  }

  private String addPrefix(String columnName) {
    return getPrefix() + columnName;
  }
}
