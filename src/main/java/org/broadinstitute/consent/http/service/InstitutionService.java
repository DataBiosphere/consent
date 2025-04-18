package org.broadinstitute.consent.http.service;

import com.google.api.client.http.HttpStatusCodes;
import com.google.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.InstitutionDomainMap;
import org.broadinstitute.consent.http.service.UserService.SimplifiedUser;

public class InstitutionService {

  private final InstitutionDAO institutionDAO;
  private final UserDAO userDAO;
  private final GCSService store;

  @Inject
  public InstitutionService(InstitutionDAO institutionDAO, UserDAO userDAO, GCSService store) {
    this.institutionDAO = institutionDAO;
    this.userDAO = userDAO;
    this.store = store;
  }

  public Institution createInstitution(Institution institution, Integer userId) {
    checkForEmptyName(institution);
    checkUserId(userId);
    Date createTimestamp = new Date();
    Integer id = institutionDAO.insertInstitution(
        institution.getName(),
        institution.getItDirectorName(),
        institution.getItDirectorEmail(),
        institution.getInstitutionUrl(),
        institution.getDunsNumber(),
        institution.getOrgChartUrl(),
        institution.getVerificationUrl(),
        institution.getVerificationFilename(),
        (Objects.nonNull(institution.getOrganizationType()) ? institution.getOrganizationType()
            .getValue() : null),
        userId,
        createTimestamp
    );
    return institutionDAO.findInstitutionById(id);
  }

  public Institution updateInstitutionById(Institution institutionPayload, Integer id,
      Integer userId) {
    Institution targetInstitution = institutionDAO.findInstitutionById(id);
    isInstitutionNull(targetInstitution);
    checkUserId(userId);
    checkForEmptyName(institutionPayload);
    Date updateDate = new Date();
    institutionDAO.updateInstitutionById(
        id,
        institutionPayload.getName(),
        institutionPayload.getItDirectorEmail(),
        institutionPayload.getItDirectorName(),
        institutionPayload.getInstitutionUrl(),
        institutionPayload.getDunsNumber(),
        institutionPayload.getOrgChartUrl(),
        institutionPayload.getVerificationUrl(),
        institutionPayload.getVerificationFilename(),
        (Objects.nonNull(institutionPayload.getOrganizationType())
            ? institutionPayload.getOrganizationType().getValue() : null),
        userId,
        updateDate
    );
    return institutionDAO.findInstitutionById(id);
  }

  public void deleteInstitutionById(Integer id) {
    Institution institution = institutionDAO.findInstitutionById(id);
    isInstitutionNull(institution);
    institutionDAO.deleteInstitutionById(id);
  }

  public Institution findInstitutionById(Integer id) {
    Institution institution = institutionDAO.findInstitutionById(id);
    isInstitutionNull(institution);

    List<SimplifiedUser> signingOfficials = userDAO.getSOsByInstitution(id).stream()
        .map(SimplifiedUser::new)
        .collect(Collectors.toList());
    institution.setSigningOfficials(signingOfficials);

    return institution;
  }

  private InstitutionDomainMap getInstitutionDomainMap() {
    try {
      return store.readJsonFileFromBucket("institution-domain/allowlist.json",
          InstitutionDomainMap.class);
    } catch (IOException e) {
      throw new ServerErrorException("Could not load institution configuration",
          HttpStatusCodes.STATUS_CODE_SERVER_ERROR, e);
    }
  }

  public Institution findInstitutionForEmail(String email) {
    String name = getInstitutionDomainMap().getInstitutionForEmail(email);
    if (name != null) {
      var institutions = institutionDAO.findInstitutionsByName(name);
      if (institutions.size() == 1) {
        return institutions.get(0);
      }
    }
    return null;
  }

  public List<Institution> findAllInstitutions() {
    return institutionDAO.findAllInstitutions();
  }

  public List<Institution> findAllInstitutionsByName(String name) {
    return institutionDAO.findInstitutionsByName(name);
  }

  private void checkForEmptyName(Institution institution) {
    String name = institution.getName();
    if (Objects.isNull(name) || name.isBlank()) {
      throw new IllegalArgumentException("Institution name cannot be null or empty");
    }
  }

  private void checkUserId(Integer userId) {
    if (Objects.isNull(userId)) {
      throw new IllegalArgumentException("User ID is a required parameter");
    }
  }

  private void isInstitutionNull(Institution institution) {
    if (Objects.isNull(institution)) {
      throw new NotFoundException("Institution not found");
    }
  }
}
