package org.broadinstitute.consent.http.models;

import java.util.Date;
import java.util.Set;
import java.util.UUID;


public interface DraftSubmissionInterface {

  String getJson();

  void setJson(String json);

  User getCreateUser();

  void setCreateUser(User user);

  User getUpdateUser();

  void setUpdateUser(User user);

  UUID getUUID();

  void setUUID(UUID uuid);

  String getName();

  void setName(String name);

  Date getCreateDate();

  void setCreateDate(Date createDate);

  Date getUpdateDate();

  void setUpdateDate(Date updateDate);

  void addStoredFile(FileStorageObject file);

  Set<FileStorageObject>getStoredFiles();

} 