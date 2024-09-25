package org.broadinstitute.consent.http.models;

import com.google.gson.Gson;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;

/**
 * DraftSubmission represents a Data Submitter's partial submission of 0 or more elements. This is
 * an internal structure, not intended to be serialized back to the client in the entirety.
 */
public class DraftSubmission implements DraftSubmissionInterface {

  private Date createDate;
  private Date updateDate;
  private Map<String, FormDataBodyPart> files;
  private Set<FileStorageObject> storedFiles;
  private String json;
  private String name;
  private User createUser;
  private User updateUser;
  private UUID uuid;

  public DraftSubmission() {
  }

  public DraftSubmission(String json, Map<String, FormDataBodyPart> fileMap, User user) {
    this.json = json;
    this.files = fileMap;
    this.createUser = user;
    this.updateUser = user;
    this.createDate = new Date();
    this.updateDate = createDate;
    this.uuid = UUID.randomUUID();
    this.name = computeDraftName(json);
  }

  @Override
  public String getJson() {
    return this.json;
  }

  @Override
  public void setJson(String json) {
    this.json = json;
  }

  @Override
  public Optional<Map<String, FormDataBodyPart>> getFiles() {
    return Optional.ofNullable(this.files);
  }

  @Override
  public User getCreateUser() {
    return this.createUser;
  }

  @Override
  public void setCreateUser(User user) {
    this.createUser = user;
  }

  @Override
  public User getUpdateUser() {
    return this.updateUser;
  }

  @Override
  public void setUpdateUser(User user) {
    this.updateUser = user;
  }

  @Override
  public UUID getUUID() {
    return this.uuid;
  }

  @Override
  public void setUUID(UUID uuid) {
    this.uuid = uuid;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public Date getCreateDate() {
    return this.createDate;
  }

  @Override
  public void setCreateDate(Date createDate) {
    this.createDate = createDate;
  }

  @Override
  public Date getUpdateDate() {
    return this.updateDate;
  }

  @Override
  public void setUpdateDate(Date updateDate) {
    this.updateDate = updateDate;
  }

  @Override
  public void addStoredFile(FileStorageObject file) {
    if (this.storedFiles == null) {
      this.storedFiles = new HashSet<>();
    }

    this.storedFiles.add(file);
  }

  @Override
  public Set<FileStorageObject> getStoredFiles() {
    return this.storedFiles;
  }

  /* Uses the provided study name or the current time to create a draft name.  */
  private String computeDraftName(String json) {
    StringBuilder name = new StringBuilder();
    Gson gson = new Gson();
    try {
      DatasetRegistrationSchemaV1 partial = gson.fromJson(json, DatasetRegistrationSchemaV1.class);
      Optional<String> studyName = Optional.ofNullable(partial.getStudyName());

      studyName.ifPresent(name::append);

      if (name.toString().trim().isEmpty()) {
        Date now = new Date();
        name.append("Created ").append(now);
      }
    } catch (Exception e) {
      Date now = new Date();
      name.append("Created ").append(now);
    }

    return name.toString();
  }
}