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
  private DataSetAssociationDAO datasetAssociationDAO;
  private DataSetAuditDAO datasetAuditDAO;
  private DataSetDAO datasetDAO;
  private ElectionDAO electionDAO;
  private HelpReportDAO helpReportDAO;
  private MailMessageDAO mailMessageDAO;
  private MailServiceDAO mailServiceDAO;
  private MatchDAO matchDAO;
  private ResearcherPropertyDAO researcherPropertyDAO;
  private UserDAO userDAO;
  private UserRoleDAO userRoleDAO;
  private VoteDAO voteDAO;
  private WorkspaceAuditDAO workspaceAuditDAO;

  public DAOContainer() {
  }

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

  public DataSetAssociationDAO getDatasetAssociationDAO() {
    return datasetAssociationDAO;
  }

  public void setDatasetAssociationDAO(
      DataSetAssociationDAO datasetAssociationDAO) {
    this.datasetAssociationDAO = datasetAssociationDAO;
  }

  public DataSetAuditDAO getDatasetAuditDAO() {
    return datasetAuditDAO;
  }

  public void setDatasetAuditDAO(DataSetAuditDAO datasetAuditDAO) {
    this.datasetAuditDAO = datasetAuditDAO;
  }

  public DataSetDAO getDatasetDAO() {
    return datasetDAO;
  }

  public void setDatasetDAO(DataSetDAO datasetDAO) {
    this.datasetDAO = datasetDAO;
  }

  public ElectionDAO getElectionDAO() {
    return electionDAO;
  }

  public void setElectionDAO(ElectionDAO electionDAO) {
    this.electionDAO = electionDAO;
  }

  public HelpReportDAO getHelpReportDAO() {
    return helpReportDAO;
  }

  public void setHelpReportDAO(HelpReportDAO helpReportDAO) {
    this.helpReportDAO = helpReportDAO;
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

  public ResearcherPropertyDAO getResearcherPropertyDAO() {
    return researcherPropertyDAO;
  }

  public void setResearcherPropertyDAO(
      ResearcherPropertyDAO researcherPropertyDAO) {
    this.researcherPropertyDAO = researcherPropertyDAO;
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
}
