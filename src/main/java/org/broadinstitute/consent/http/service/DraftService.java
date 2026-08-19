package org.broadinstitute.consent.http.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.DraftDAO;
import org.broadinstitute.consent.http.models.DraftInterface;
import org.broadinstitute.consent.http.models.DraftSummary;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.dao.DraftServiceDAO;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.jdbi.v3.core.Jdbi;

public class DraftService implements ConsentLogger {

  private final DraftDAO draftDAO;
  private final DraftServiceDAO draftServiceDAO;
  private final GCSService gcsService;

  @Inject
  public DraftService(Jdbi jdbi, DraftServiceDAO draftServiceDAO, GCSService gcsService) {
    this.draftDAO = jdbi.onDemand(DraftDAO.class);
    this.draftServiceDAO = draftServiceDAO;
    this.gcsService = gcsService;
  }

  public DraftInterface getAuthorizedDraft(UUID uuid, User user) {
    return draftServiceDAO.getAuthorizedDraft(uuid, user);
  }

  public List<FileStorageObject> addAttachments(
      DraftInterface draft, User user, Map<String, FormDataBodyPart> files)
      throws SQLException, RuntimeException {
    return draftServiceDAO.addAttachments(draft, user, files);
  }

  public InputStream getDraftAttachmentStream(FileStorageObject targetAttachment) {
    return gcsService.getDocument(targetAttachment.getBlobId());
  }

  public void deleteDraftAttachment(DraftInterface draft, User user, Integer fileId)
      throws SQLException {
    draftServiceDAO.deleteDraftAttachment(draft, user, fileId);
  }

  public StreamingOutput draftAsJson(DraftInterface draft) {
    Gson gson = GsonUtil.buildGson();
    JsonObject meta = gson.toJsonTree(draft).getAsJsonObject();
    // Taken from the draft rather than from whatever its class serializes, so every type reports
    // itself and no client has to infer one from the UUID or the route.
    meta.addProperty("draftType", draft.getType().getValue());
    return output -> {
      output.write("{ \"document\":".getBytes(StandardCharsets.UTF_8));
      output.write(draft.getJson().getBytes(StandardCharsets.UTF_8));
      output.write(", \"meta\":".getBytes(StandardCharsets.UTF_8));
      output.write(gson.toJson(meta).getBytes(StandardCharsets.UTF_8));
      output.write("}".getBytes(StandardCharsets.UTF_8));
    };
  }

  public void insertDraft(DraftInterface draft) throws SQLException {
    draftServiceDAO.insertDraft(draft);
  }

  public Collection<DraftSummary> findDraftSummariesForUser(User user) {
    return draftDAO.findDraftSummariesByUserId(user.getUserId());
  }

  public DraftInterface updateDraft(DraftInterface draft, User user) throws SQLException {
    return draftServiceDAO.updateDraft(draft, user);
  }

  public void deleteDraft(DraftInterface draft, User user) throws SQLException, NotFoundException {
    draftServiceDAO.deleteDraft(draft, user);
  }
}
