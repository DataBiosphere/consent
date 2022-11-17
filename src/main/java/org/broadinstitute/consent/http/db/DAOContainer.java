package org.broadinstitute.consent.http.db;

/**
 * Generic container class for DAOs that can be used in service constructors to simplify instance
 * creation.
 */
@SuppressWarnings("unused")
public class DAOContainer {

  private ConsentDAO consentDAO;
  private CounterDAO counterDAO;
  private DacDAO dacDAO;
  private DataAccessRequestDAO dataAccessRequestDAO;
  private DarCollectionDAO darCollectionDAO;
  private DarCollectionSummaryDAO darCollectionSummaryDAO;
  private DatasetAssociationDAO datasetAssociationDAO;
  private DatasetDAO datasetDAO;
  private ElectionDAO electionDAO;
  private MailMessageDAO mailMessageDAO;
  private MatchDAO matchDAO;
  private UserPropertyDAO userPropertyDAO;
  private UserDAO userDAO;
  private UserRoleDAO userRoleDAO;
  private VoteDAO voteDAO;
  private ConsentAuditDAO consentAuditDAO;
  private InstitutionDAO institutionDAO;
  private UserFileDAO userFileDAO;
  private AcknowledgementDAO acknowledgementDAO;

  public ConsentDAO getConsentDAO() {
    return consentDAO;
  }

  public void setConsentDAO(ConsentDAO consentDAO) {
    this.consentDAO = consentDAO;
  }

  public CounterDAO getCounterDAO() {
    return counterDAO;
  }

  public void setCounterDAO(CounterDAO counterDAO) {
    this.counterDAO = counterDAO;
  }

  public DacDAO getDacDAO() {
    return dacDAO;
  }

  public void setDacDAO(DacDAO dacDAO) {
    this.dacDAO = dacDAO;
  }

  public DataAccessRequestDAO getDataAccessRequestDAO() {
    return dataAccessRequestDAO;
  }

  public void setDataAccessRequestDAO(
      DataAccessRequestDAO dataAccessRequestDAO) {
    this.dataAccessRequestDAO = dataAccessRequestDAO;
  }

  public DarCollectionDAO getDarCollectionDAO() {
    return darCollectionDAO;
  }

  public DarCollectionSummaryDAO getDarCollectionSummaryDAO() {
    return darCollectionSummaryDAO;
  }

  public void setDarCollectionDAO(
    DarCollectionDAO darCollectionDAO) {
    this.darCollectionDAO = darCollectionDAO;
  }

  public void setDarCollectionSummaryDAO(
    DarCollectionSummaryDAO darCollectionSummaryDAO
  ) {
    this.darCollectionSummaryDAO = darCollectionSummaryDAO;
  }

  public DatasetAssociationDAO getDatasetAssociationDAO() {
    return datasetAssociationDAO;
  }

  public void setDatasetAssociationDAO(
      DatasetAssociationDAO datasetAssociationDAO) {
    this.datasetAssociationDAO = datasetAssociationDAO;
  }

  public DatasetDAO getDatasetDAO() {
    return datasetDAO;
  }

  public void setDatasetDAO(DatasetDAO datasetDAO) {
    this.datasetDAO = datasetDAO;
  }

  public ElectionDAO getElectionDAO() {
    return electionDAO;
  }

  public void setElectionDAO(ElectionDAO electionDAO) {
    this.electionDAO = electionDAO;
  }

  public MailMessageDAO getMailMessageDAO() {
    return mailMessageDAO;
  }

  public void setMailMessageDAO(MailMessageDAO mailMessageDAO) {
    this.mailMessageDAO = mailMessageDAO;
  }

  public MatchDAO getMatchDAO() {
    return matchDAO;
  }

  public void setMatchDAO(MatchDAO matchDAO) {
    this.matchDAO = matchDAO;
  }

  public UserPropertyDAO getResearcherPropertyDAO() {
    return userPropertyDAO;
  }

  public void setResearcherPropertyDAO(
      UserPropertyDAO userPropertyDAO) {
    this.userPropertyDAO = userPropertyDAO;
  }

  public UserDAO getUserDAO() {
    return userDAO;
  }

  public void setUserDAO(UserDAO userDAO) {
    this.userDAO = userDAO;
  }

  public UserRoleDAO getUserRoleDAO() {
    return userRoleDAO;
  }

  public void setUserRoleDAO(UserRoleDAO userRoleDAO) {
    this.userRoleDAO = userRoleDAO;
  }

  public VoteDAO getVoteDAO() {
    return voteDAO;
  }

  public void setVoteDAO(VoteDAO voteDAO) {
    this.voteDAO = voteDAO;
  }

  public ConsentAuditDAO getConsentAuditDAO() {
    return consentAuditDAO;
  }

  public void setConsentAuditDAO(ConsentAuditDAO consentAuditDAO) {
    this.consentAuditDAO = consentAuditDAO;
  }

  public InstitutionDAO getInstitutionDAO() { return institutionDAO; }

  public void setInstitutionDAO(InstitutionDAO institutionDAO) { this.institutionDAO = institutionDAO; }
  public UserFileDAO getUserFileDAO() { return userFileDAO; }
  public void setUserFileDAO(UserFileDAO userFileDAO) { this.userFileDAO = userFileDAO; }

  public AcknowledgementDAO getAcknowledgementDAO() {
    return acknowledgementDAO;
  }

  public void setAcknowledgementDAO(AcknowledgementDAO acknowledgementDAO) {
    this.acknowledgementDAO = acknowledgementDAO;
  }
}
