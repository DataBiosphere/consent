package org.broadinstitute.consent.http.service;

import com.google.gson.Gson;
import com.google.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.broadinstitute.consent.http.db.DraftDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DraftInterface;
import org.broadinstitute.consent.http.models.DraftSummary;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.jdbi.v3.core.Jdbi;

public class DraftService {

  private final Jdbi jdbi;
  private final DraftDAO draftDAO;
  private final DraftFileStorageService draftFileStorageService;

  @Inject
  public DraftService(Jdbi jdbi, DraftDAO draftDAO,
      DraftFileStorageService draftFileStorageService) {
    this.jdbi = jdbi;
    this.draftDAO = draftDAO;
    this.draftFileStorageService = draftFileStorageService;
  }

  public void insertDraft(DraftInterface draft)
      throws SQLException, BadRequestException {
    jdbi.useHandle(handle -> {
      handle.getConnection().setAutoCommit(false);
      try {
        draftDAO.insert(draft.getName(), draft.getCreateDate().toInstant(),
            draft.getCreateUser().getUserId(), draft.getJson(), draft.getUUID(),
            draft.getClass().getName());
      } catch (Exception e) {
        handle.rollback();
        throw new BadRequestException(
            "Error submitting draft.  Drafts require valid json to be submitted.");
      }
      handle.commit();
    });
  }

  public void updateDraft(DraftInterface draft, User user) throws SQLException {
    draft.setUpdateUser(user);
    draft.setUpdateDate(new Date());
    jdbi.useHandle(handle -> {
      handle.getConnection().setAutoCommit(false);
      try {
        draftDAO.updateDraftByDraftUUID(draft.getName(),
            draft.getUpdateDate().toInstant(), draft.getUpdateUser().getUserId(), draft.getJson(),
            draft.getUUID(), draft.getClass().getName());
      } catch (Exception e) {
        handle.rollback();
      }
      handle.commit();
    });
  }

  public DraftInterface getAuthorizedDraft(UUID draftUUID, User user) {
    DraftInterface draft;
    try {
      draft = findDraftByDraftUUID(draftUUID);
    } catch (SQLException e) {
      throw new NotFoundException(
          String.format("Draft with UUID %s not found.", draftUUID.toString()));
    }
    if (Objects.isNull(draft)) {
      throw new NotFoundException(
          String.format("Draft with UUID %s not found.", draftUUID.toString()));
    }
    if (!user.getUserId().equals(draft.getCreateUser().getUserId()) && !user.hasUserRole(
        UserRoles.ADMIN)) {
      throw new NotAuthorizedException("User not authorized to modify resource.");
    }
    return draft;
  }

  public void deleteDraftsByUser(User user) {
    Set<DraftInterface> userDrafts = findDraftsForUser(user);
    for (DraftInterface draft : userDrafts) {
      deleteDraft(draft, user);
    }
  }

  public Set<DraftSummary> findDraftSummariesForUser(User user) {
    return draftDAO.findDraftSummariesByUserId(user.getUserId());
  }

  public Set<DraftInterface> findDraftsForUser(User user) {
    return draftDAO.findDraftsByUserId(user.getUserId());
  }

  private DraftInterface findDraftByDraftUUID(
      UUID draftSubmissionUUID) throws SQLException {
    return draftDAO.findDraftById(draftSubmissionUUID);
  }

  public DraftInterface addAttachments(DraftInterface draft, User user,
      Map<String, FormDataBodyPart> files) throws SQLException {
    draftFileStorageService.storeDraftFiles(draft.getUUID(), user, files);
    draftDAO.updateDraftByDraftUUID(draft.getUUID(),
        new Date().toInstant(), user.getUserId());
    return findDraftByDraftUUID(draft.getUUID());
  }

  public void deleteDraftAttachment(DraftInterface draft, User user, Integer fileId)
      throws SQLException {
    Optional<FileStorageObject> fileStorageObjectToDelete = draft.getStoredFiles().stream()
        .filter(fileStorageObject -> fileStorageObject.getFileStorageObjectId().equals(fileId))
        .findFirst();
    if (fileStorageObjectToDelete.isPresent()) {
      draftFileStorageService.deleteStoredFile(fileStorageObjectToDelete.get(), user);
      draftDAO.updateDraftByDraftUUID(draft.getUUID(),
          new Date().toInstant(), user.getUserId());
    } else {
      throw new NotFoundException(
          String.format("Draft attachment is not found.  Draft: %s, Attachment: %d",
              draft.getUUID(), fileId));
    }
  }

  public void deleteDraft(DraftInterface draft, User user)
      throws RuntimeException {
    jdbi.useHandle(handle -> {
      try {
        handle.useTransaction(handler -> {
          draftDAO.deleteDraftByUUIDList(List.of(draft.getUUID()));
          draft.getStoredFiles().forEach(fileStorageObject -> {
            try {
              draftFileStorageService.deleteStoredFile(fileStorageObject, user);
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
          });
        });
      } catch (Exception e) {
        handle.rollback();
      }
      handle.commit();
    });
  }

  public StreamingOutput draftAsJson(DraftInterface draft) {
    Gson gson = GsonUtil.buildGson();
    return output -> {
      output.write("{ \"document\":".getBytes());
      output.write(draft.getJson().getBytes());
      output.write(", \"meta\":".getBytes());
      output.write(gson.toJson(draft).getBytes());
      output.write("}".getBytes());
    };
  }

  public InputStream getDraftAttachmentStream(FileStorageObject targetAttachment) {
    return draftFileStorageService.get(targetAttachment);
  }
}
