package org.broadinstitute.consent.http.db;

/**
 * Generic container class for DAOs that can be used in service constructors to simplify instance
 * creation.
 */
@SuppressWarnings("unused")
public class DAOContainer {

  private ApprovalExpirationTimeDAO approvalExpirationTimeDAO;
  private AssociationDAO associationDAO;
  private ConsentDAO consentDAO;
  private CounterDAO counterDAO;
  private DacDAO dacDAO;
  private DataAccessRequestDAO dataAccessRequestDAO;
  private DatasetAssociationDAO datasetAssociationDAO;
  private DatasetDAO datasetDAO;
  private ElectionDAO electionDAO;
  private MailMessageDAO mailMessageDAO;
  private MailServiceDAO mailServiceDAO;
  private MatchDAO matchDAO;
  private UserPropertyDAO userPropertyDAO;
  private UserDAO userDAO;
  private UserRoleDAO userRoleDAO;
  private VoteDAO voteDAO;
  private WorkspaceAuditDAO workspaceAuditDAO;
  private InstitutionDAO institutionDAO;

  public ApprovalExpirationTimeDAO getApprovalExpirationTimeDAO() {
    return approvalExpirationTimeDAO;
  }

  public void setApprovalExpirationTimeDAO(
      ApprovalExpirationTimeDAO approvalExpirationTimeDAO) {
    this.approvalExpirationTimeDAO = approvalExpirationTimeDAO;
  }

  public AssociationDAO getAssociationDAO() {
    return associationDAO;
  }

  public void setAssociationDAO(AssociationDAO associationDAO) {
    this.associationDAO = associationDAO;
  }

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

  public MailServiceDAO getMailServiceDAO() {
    return mailServiceDAO;
  }

  public void setMailServiceDAO(MailServiceDAO mailServiceDAO) {
    this.mailServiceDAO = mailServiceDAO;
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

  public WorkspaceAuditDAO getWorkspaceAuditDAO() {
    return workspaceAuditDAO;
  }

  public void setWorkspaceAuditDAO(WorkspaceAuditDAO workspaceAuditDAO) {
    this.workspaceAuditDAO = workspaceAuditDAO;
  }

  public InstitutionDAO getInstitutionDAO() { return institutionDAO; }

  public void setInstitutionDAO(InstitutionDAO institutionDAO) { this.institutionDAO = institutionDAO; }
}
