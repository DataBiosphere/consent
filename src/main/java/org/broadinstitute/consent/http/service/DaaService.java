package org.broadinstitute.consent.http.service;

import com.google.cloud.storage.BlobId;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.DaaDAO;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.UserService.SimplifiedUser;
import org.broadinstitute.consent.http.service.dao.DaaServiceDAO;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

public class DaaService implements ConsentLogger {

  private final DaaServiceDAO daaServiceDAO;
  private final DaaDAO daaDAO;
  private final GCSService gcsService;
  private final EmailService emailService;
  private final InstitutionDAO institutionDAO;

  @Inject
  public DaaService(DaaServiceDAO daaServiceDAO, DaaDAO daaDAO, GCSService gcsService, EmailService emailService, InstitutionDAO institutionDAO) {
    this.daaServiceDAO = daaServiceDAO;
    this.daaDAO = daaDAO;
    this.gcsService = gcsService;
    this.emailService = emailService;
    this.institutionDAO = institutionDAO;
  }

  /**
   * Create a new DataAccessAgreement with file content.
   *
   * @param userId The create User ID
   * @param dacId The initial DAC ID
   * @param inputStream The file content
   * @param fileDetail The file details
   * @return The created DataAccessAgreement
   * @throws ServerErrorException The Exception
   */
  public DataAccessAgreement createDaaWithFso(Integer userId, Integer dacId,
      InputStream inputStream,
      FormDataContentDisposition fileDetail)
      throws ServerErrorException {
    UUID id = UUID.randomUUID();
    BlobId blobId;
    try {
      blobId = gcsService.storeDocument(inputStream, fileDetail.getType(), id);
    } catch (IOException e) {
      logException(String.format("Error storing DAA file in GCS. User ID: %s; Dac ID: %s. ", userId, dacId), e);
      throw new ServerErrorException("Error storing DAA file in GCS.", 500);
    }
    Integer daaId;
    try {
      String mediaType = switch (StringUtils.substringAfterLast(fileDetail.getFileName(), ".")) {
        case "png", "gif", "jpg", "jpeg" -> "image";
        default -> MediaType.APPLICATION_OCTET_STREAM;
      };
      FileStorageObject fso = new FileStorageObject();
      fso.setBlobId(blobId);
      fso.setFileName(fileDetail.getFileName());
      fso.setCategory(FileCategory.DATA_ACCESS_AGREEMENT);
      fso.setMediaType(mediaType);
      daaId = daaServiceDAO.createDaaWithFso(userId, dacId, fso);
    } catch (Exception e) {
      try {
        gcsService.deleteDocument(blobId.getName());
      } catch (Exception ex) {
        logException(String.format("Error deleting DAA file from GCS. User ID: %s; Dac ID: %s. ", userId, dacId), ex);
      }
      logException(String.format("Error saving DAA. User ID: %s; Dac ID: %s. ", userId, dacId), e);
      throw new ServerErrorException("Error saving DAA.", 500);
    }
    return daaDAO.findById(daaId);
  }

  public void addDacToDaa(Integer dacId, Integer daaId) {
    daaDAO.createDacDaaRelation(dacId, daaId);
  }

  public void removeDacFromDaa(Integer dacId, Integer daaId) {
    daaDAO.deleteDacDaaRelation(dacId, daaId);
  }

  public List<DataAccessAgreement> findAll() {
    List<DataAccessAgreement> daas = daaDAO.findAll();
    if (daas != null) {
      return daas;
    }
    return List.of();
  }

  public DataAccessAgreement findById(Integer daaId) {
    DataAccessAgreement daa = daaDAO.findById(daaId);
    if (daa != null) {
      return daa;
    }
    throw new NotFoundException("Could not find DAA with the provided ID: " + daaId);
  }

  public void sendDaaRequestEmails(User user, Integer daaId) throws Exception {
    try {
      Institution institution = institutionDAO.findInstitutionWithSOById(user.getInstitutionId());
      if (institution == null) {
        throw new BadRequestException("This user has not set their institution: " + user.getDisplayName());
      }
      List<SimplifiedUser> signingOfficials = institution.getSigningOfficials();
      if (signingOfficials.isEmpty()) {
        throw new NotFoundException("No signing officials found for user: " + user.getDisplayName());
      }
      int userId = user.getUserId();
      String userName = user.getDisplayName();
      for (SimplifiedUser signingOfficial : signingOfficials) {
        DataAccessAgreement daa = findById(daaId);
        String daaName = daa.getFile().getFileName();
        emailService.sendDaaRequestMessage( signingOfficial.displayName, signingOfficial.email,
            userName, daaName, daaId, userId);
      }
    } catch (Exception e) {
      logException(e);
      throw(e);
    }
  }

  public void sendNewDaaEmails(User user, Integer daaId) throws Exception {
    try {
      // this will be used by a DAC Chair
      // i want to get the dac details from this user
      // once i get the dac id, i need to find all SOs or just the SOs that have issues LC-DAA relationships with this DAC?
      // once i get the dac id, i need to find all researchers who have requested data from this DAC
//      Institution institution = institutionDAO.findInstitutionWithSOById(user.getInstitutionId());
//      if (institution == null) {
//        throw new BadRequestException("This user has not set their institution: " + user.getDisplayName());
//      }
      List<SimplifiedUser> signingOfficials = institution.getSigningOfficials();
      if (signingOfficials.isEmpty()) {
        throw new NotFoundException("No signing officials found for user: " + user.getDisplayName());
      }
      int userId = user.getUserId();
      String userName = user.getDisplayName();
      for (SimplifiedUser signingOfficial : signingOfficials) {
        DataAccessAgreement daa = findById(daaId);
        String daaName = daa.getFile().getFileName();
        emailService.sendDaaRequestMessage( signingOfficial.displayName, signingOfficial.email,
            userName, daaName, daaId, userId);
      }
    } catch (Exception e) {
      logException(e);
      throw(e);
    }
  }

  public InputStream findFileById(Integer daaId) {
    DataAccessAgreement daa = daaDAO.findById(daaId);
    if (daa != null) {
      FileStorageObject file = daa.getFile();
      if (file != null) {
        return gcsService.getDocument(file.getBlobId().getName());
      }
    }
    throw new NotFoundException("Could not find DAA File with the provided ID: " + daaId);
  }

  public List<DataAccessAgreement> findDAAsInJsonArray(String json, String arrayKey) {
    List<JsonElement> jsonElementList;
    try {
      JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
      jsonElementList = jsonObject.getAsJsonArray(arrayKey).asList();
    } catch (Exception e) {
      throw new BadRequestException("Invalid JSON or missing array with key: " + arrayKey);
    }
    return jsonElementList.stream().distinct().map(e -> findById(e.getAsInt())).toList();
  }

  public void deleteDaa(Integer daaId) {
    DataAccessAgreement daa = daaDAO.findById(daaId);
    if (daa != null) {
      daaDAO.deleteDaa(daaId);
    } else {
      throw new NotFoundException("Could not find DAA with the provided ID: " + daaId);
    }
  }
}
