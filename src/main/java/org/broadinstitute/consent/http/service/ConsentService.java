package org.broadinstitute.consent.http.service;

import jakarta.ws.rs.NotFoundException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.exceptions.UnknownIdentifierException;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Election;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsentService {

  private final Logger logger;
  private final ConsentDAO consentDAO;
  private final ElectionDAO electionDAO;
  private final UseRestrictionConverter useRestrictionConverter;

  @Inject
  public ConsentService(ConsentDAO consentDAO, ElectionDAO electionDAO,
      UseRestrictionConverter useRestrictionConverter) {
    this.consentDAO = consentDAO;
    this.electionDAO = electionDAO;
    this.useRestrictionConverter = useRestrictionConverter;
    this.logger = LoggerFactory.getLogger(this.getClass());
  }

  public Consent create(Consent rec) {
    String id;
    if (StringUtils.isNotEmpty(rec.consentId)) {
      id = rec.consentId;
    } else {
      id = UUID.randomUUID().toString();
    }
    if (consentDAO.getIdByName(rec.getName()) != null) {
      throw new IllegalArgumentException("Consent for the specified name already exist");
    }
    if (StringUtils.isNotEmpty(rec.consentId)
        && consentDAO.checkConsentById(rec.consentId) != null) {
      throw new IllegalArgumentException("Consent for the specified id already exist");
    }
    Date createDate = new Date();
    if (Objects.isNull(rec.getTranslatedUseRestriction()) && Objects.nonNull(rec.getDataUse())) {
      String translatedUseRestriction = useRestrictionConverter.translateDataUse(rec.getDataUse(),
          DataUseTranslationType.DATASET);
      rec.setTranslatedUseRestriction(translatedUseRestriction);
    }
    consentDAO.insertConsent(id, rec.getRequiresManualReview(),
        rec.getDataUse().toString(),
        rec.getDataUseLetter(), rec.getName(), rec.getDulName(), createDate, createDate,
        rec.getTranslatedUseRestriction(), rec.getGroupName());
    return consentDAO.findConsentById(id);
  }

  public Consent update(String id, Consent rec) throws NotFoundException {
    rec = updateConsentDates(rec);
    if (StringUtils.isEmpty(consentDAO.checkConsentById(id))) {
      throw new NotFoundException();
    }
    if (Objects.isNull(rec.getTranslatedUseRestriction()) && Objects.nonNull(rec.getDataUse())) {
      rec.setTranslatedUseRestriction(useRestrictionConverter.translateDataUse(rec.getDataUse(),
          DataUseTranslationType.DATASET));
    }
    consentDAO.updateConsent(id, rec.getRequiresManualReview(),
        rec.getDataUse().toString(),
        rec.getDataUseLetter(), rec.getName(), rec.getDulName(), rec.getLastUpdate(),
        rec.getSortDate(), rec.getTranslatedUseRestriction(), rec.getGroupName(), true);
    return consentDAO.findConsentById(id);
  }

  public Consent retrieve(String id) throws UnknownIdentifierException {
    Consent consent = consentDAO.findConsentById(id);
    if (consent == null) {
      throw new UnknownIdentifierException(String.format("Could not find consent with id %s", id));
    }

    Election election = electionDAO.findLastElectionByReferenceIdAndType(id,
        ElectionType.TRANSLATE_DUL.getValue());
    if (election != null) {
      consent.setLastElectionStatus(election.getStatus());
      consent.setLastElectionArchived(election.getArchived());
    }
    return consent;
  }

  public void delete(String id) throws IllegalArgumentException {
    checkConsentExists(id);
    List<Election> elections = electionDAO.findElectionsWithFinalVoteByReferenceId(id);
    if (elections.isEmpty()) {
      consentDAO.deleteConsent(id);
      consentDAO.deleteAllAssociationsForConsent(id);
    } else {
      throw new IllegalArgumentException(
          "Consent cannot be deleted because already exist elections associated with it");
    }
  }

  // Check that the specified Consent resource exists, or throw NotFoundException.
  private void checkConsentExists(String consentId) {
    String ck_id = consentDAO.checkConsentById(consentId);
    logger.debug(String.format("CreateAssocition, checkConsentbyId returned '%s'",
        (ck_id == null ? "<null>" : ck_id)));
    if (ck_id == null) {
      throw new NotFoundException(String.format("Consent with id '%s' not found", consentId));
    }
  }

  private Consent updateConsentDates(Consent c) {
    Timestamp updateDate = new Timestamp(new Date().getTime());
    c.setLastUpdate(updateDate);
    c.setSortDate(updateDate);
    return c;
  }

  public Consent getByName(String name) throws UnknownIdentifierException {
    Consent consent = consentDAO.findConsentByName(name);
    if (consent == null) {
      throw new UnknownIdentifierException("Consent does not exist");
    }
    return consent;
  }
}
