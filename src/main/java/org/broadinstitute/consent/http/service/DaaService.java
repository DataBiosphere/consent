package org.broadinstitute.consent.http.service;

import com.google.cloud.storage.BlobId;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import freemarker.template.TemplateException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.DaaDAO;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.mail.message.NewDAAUploadResearcherMessage;
import org.broadinstitute.consent.http.mail.message.NewDAAUploadSOMessage;
import org.broadinstitute.consent.http.models.DaaBulkAssignmentResult;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.UserService.SimplifiedUser;
import org.broadinstitute.consent.http.service.dao.DaaServiceDAO;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.jdbi.v3.core.Jdbi;

public class DaaService implements ConsentLogger {

  private final DaaServiceDAO daaServiceDAO;
  private final DaaDAO daaDAO;
  private final GCSService gcsService;
  private final EmailService emailService;
  private final UserService userService;
  private final DacDAO dacDAO;
  private final LibraryCardService libraryCardService;

  @Inject
  public DaaService(
      Jdbi jdbi,
      DaaServiceDAO daaServiceDAO,
      GCSService gcsService,
      EmailService emailService,
      UserService userService,
      LibraryCardService libraryCardService) {
    this.daaServiceDAO = daaServiceDAO;
    this.daaDAO = jdbi.onDemand(DaaDAO.class);
    this.gcsService = gcsService;
    this.emailService = emailService;
    this.userService = userService;
    this.dacDAO = jdbi.onDemand(DacDAO.class);
    this.libraryCardService = libraryCardService;
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
  public DataAccessAgreement createDaaWithFso(
      Integer userId, Integer dacId, InputStream inputStream, FormDataContentDisposition fileDetail)
      throws ServerErrorException {
    UUID id = UUID.randomUUID();
    BlobId blobId;
    try {
      blobId = gcsService.storeDocument(inputStream, fileDetail.getType(), id);
    } catch (IOException e) {
      logException(
          String.format("Error storing DAA file in GCS. User ID: %s; Dac ID: %s. ", userId, dacId),
          e);
      throw new ServerErrorException("Error storing DAA file in GCS.", 500);
    }
    Integer daaId;
    try {
      String mediaType =
          switch (StringUtils.substringAfterLast(fileDetail.getFileName(), ".")) {
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
        logException(
            String.format(
                "Error deleting DAA file from GCS. User ID: %s; Dac ID: %s. ", userId, dacId),
            ex);
      }
      logException(String.format("Error saving DAA. User ID: %s; Dac ID: %s. ", userId, dacId), e);
      throw new ServerErrorException("Error saving DAA.", 500);
    }
    return daaDAO.findById(daaId);
  }

  public void addDacToDaa(Integer userId, Integer dacId, Integer daaId) {
    daaDAO.createDacDaaRelation(dacId, daaId, userId);
  }

  public void removeDacFromDaa(Integer userId, Integer dacId, Integer daaId) {
    daaDAO.deleteDacDaaRelation(daaId, dacId, userId);
  }

  // Note: This method/implementation is not the permanent solution to identifying the Broad DAA.
  // Work is ticketed to refactor this logic.
  public boolean isBroadDAA(int daaId, List<DataAccessAgreement> allDaas, List<Dac> allDacs) {
    // Artificially tag the Broad/DUOS DAA as a reference DAA.
    Optional<Dac> broadDac =
        allDacs.stream().filter(dac -> dac.getName().toLowerCase().contains("broad")).findFirst();
    if (broadDac.isPresent()) {
      Optional<DataAccessAgreement> broadDAA =
          allDaas.stream()
              .filter(daa -> daa.getInitialDacId().equals(broadDac.get().getDacId()))
              .findFirst();
      broadDAA.ifPresent(daa -> daa.setBroadDaa(true));
    } else {
      // In this case, we want the first created DAA to be the Broad default DAA.
      allDaas.stream()
          .min(Comparator.comparing(DataAccessAgreement::getDaaId))
          .ifPresent(daa -> daa.setBroadDaa(true));
    }
    Optional<DataAccessAgreement> broadDaa =
        allDaas.stream().filter(daa -> daa.getDaaId().equals(daaId)).findFirst();
    if (broadDaa.isPresent()) {
      return broadDaa.get().getBroadDaa();
    } else {
      return false;
    }
  }

  public List<DataAccessAgreement> findAll() {
    List<DataAccessAgreement> daas = daaDAO.findAll();
    List<Dac> allDacs = dacDAO.findAll();
    if (daas != null) {
      daas.forEach(daa -> daa.setBroadDaa(isBroadDAA(daa.getDaaId(), daas, allDacs)));
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

  /**
   * Returns all DAA IDs associated to a DAC, including both explicit dac_daa links and DAAs whose
   * initial_dac_id matches the DAC.
   */
  public List<Integer> findDaaIdsByDacId(Integer dacId) {
    List<Integer> daaIds = daaDAO.findDaaIdsByDacId(dacId);
    return daaIds == null ? List.of() : daaIds;
  }

  /** Returns true when the provided DAA is associated to the DAC (directly or as initial DAC). */
  public boolean isDaaLinkedToDac(Integer dacId, Integer daaId) {
    return daaDAO.isDaaLinkedToDac(dacId, daaId) || daaDAO.isDaaInitiallyLinkedToDac(dacId, daaId);
  }

  /** Creates a new DAA for the DAC and links it for DAC document upload workflows. */
  public Integer createAndLinkDaaIdForDac(User user, Integer dacId) {
    Instant now = Instant.now();
    Integer daaId = daaDAO.createDaa(user.getUserId(), now, user.getUserId(), now, dacId);
    addDacToDaa(user.getUserId(), dacId, daaId);
    return daaId;
  }

  public void sendNewDaaEmails(User user, Integer daaId, String dacName, String newDaaName)
      throws Exception {
    try {
      DataAccessAgreement daa = findById(daaId);
      if (daa != null) {
        String previousDaaName = daa.getFile().getFileName();
        List<SimplifiedUser> researchers = userService.getUsersByDaaId(daaId);
        List<SimplifiedUser> signingOfficials =
            researchers.stream()
                .flatMap(
                    researcher ->
                        userService.findSOsByInstitutionId(researcher.getInstitutionId()).stream())
                .distinct()
                .toList();
        User toUser = new User();

        for (SimplifiedUser researcher : researchers) {
          toUser.setEmail(researcher.getEmail());
          toUser.setDisplayName(researcher.getDisplayName());
          sendNewDAAUploadResearcherMessage(
              toUser, dacName, previousDaaName, newDaaName, user.getUserId());
        }
        for (SimplifiedUser signingOfficial : signingOfficials) {
          toUser.setEmail(signingOfficial.getEmail());
          toUser.setDisplayName(signingOfficial.getDisplayName());
          sendNewDAAUploadSOMessage(toUser, dacName, previousDaaName, newDaaName, user.getUserId());
        }
      }
    } catch (Exception e) {
      logException(e);
      throw (e);
    }
  }

  @VisibleForTesting
  protected void sendNewDAAUploadResearcherMessage(
      User researcher, String dacName, String previousDaaName, String newDaaName, Integer userId)
      throws TemplateException, IOException {
    emailService.sendMessage(
        new NewDAAUploadResearcherMessage(researcher, dacName, previousDaaName, newDaaName),
        userId);
  }

  @VisibleForTesting
  protected void sendNewDAAUploadSOMessage(
      User signingOfficial,
      String dacName,
      String previousDaaName,
      String newDaaName,
      Integer userId)
      throws TemplateException, IOException {
    emailService.sendMessage(
        new NewDAAUploadSOMessage(signingOfficial, dacName, previousDaaName, newDaaName), userId);
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
    } catch (Exception _) {
      throw new BadRequestException("Invalid JSON or missing array with key: " + arrayKey);
    }
    return jsonElementList.stream().distinct().map(e -> findById(e.getAsInt())).toList();
  }

  public List<DataAccessAgreement> findByDarReferenceId(String referenceId) {
    return daaDAO.findByDarReferenceId(referenceId);
  }

  public Map<Integer, Set<Integer>> findDaaIdsByDatasetIds(Set<Integer> datasetIds) {
    return daaDAO.mapDaaIdsToDatasetIds(datasetIds);
  }

  /**
   * Bulk assign a Data Access Agreement to all DUOS users who have both an institution and a
   * library card assigned.
   *
   * @param daaId The DAA ID to assign
   * @param admin The user performing the bulk assignment (will be recorded in audit)
   * @return DaaBulkAssignmentResult containing assignment statistics and any errors
   */
  public DaaBulkAssignmentResult assignDaaToAllEligibleUsers(Integer daaId, User admin) {
    // Validate that the DAA exists
    findById(daaId);

    // Get all users with both institution and library card assigned
    List<User> eligibleUsers = userService.findAllUsersWithInstitutionAndLibraryCard();

    int assignedCount = 0;
    int skippedCount = 0;
    List<String> errors = new ArrayList<>();

    for (User user : eligibleUsers) {
      try {
        if (user.getLibraryCard() == null || user.getLibraryCard().getId() == null) {
          skippedCount++;
          continue;
        }
        // Users are pre-filtered to have an institution and an existing library card.
        libraryCardService.addDaaToLibraryCard(
            user.getUserId(), admin.getUserId(), user.getLibraryCard().getId(), daaId);
        assignedCount++;
      } catch (Exception e) {
        // Log the error but continue processing other users
        String errorMsg =
            String.format(
                "Failed to assign DAA %d to user %d (%s): %s",
                daaId, user.getUserId(), user.getEmail(), e.getMessage());
        logWarn(errorMsg);
        errors.add(errorMsg);
        skippedCount++;
      }
    }

    logInfo(
        String.format(
            "Bulk DAA assignment completed. DAA ID: %d, Total users eligible: %d, Assigned: %d, Skipped: %d",
            daaId, eligibleUsers.size(), assignedCount, skippedCount));

    return new DaaBulkAssignmentResult(
        daaId, eligibleUsers.size(), assignedCount, skippedCount, errors);
  }
}
