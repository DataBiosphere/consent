package org.broadinstitute.consent.http.models;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;


public interface DraftSubmissionInterface {

  public String getJson();

  public void setJson(String json);

  public Optional<Map<String, FormDataBodyPart>> getFiles();

  public void setCreateUser(User user);

  public User getCreateUser();

  public void setUpdateUser(User user);

  public User getUpdateUser();

  public UUID getUUID();

  public void setUUID(UUID uuid);

  public void setName(String name);

  public String getName();

  public Date getCreateDate();

  public void setCreateDate(Date createDate);

  public Date getUpdateDate();

  public void setUpdateDate(Date updateDate);

  public void addStoredFile(FileStorageObject file);

  public Set<FileStorageObject> getStoredFiles();

} 