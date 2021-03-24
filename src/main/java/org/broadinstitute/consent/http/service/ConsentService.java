package org.broadinstitute.consent.http.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.AssociationDAO;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.Actions;
import org.broadinstitute.consent.http.enumeration.AssociationType;
import org.broadinstitute.consent.http.enumeration.AuditTable;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ConsentAssociation;
import org.broadinstitute.consent.http.models.ConsentManage;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DatasetDetailEntry;
import org.broadinstitute.consent.http.models.Election;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsentService {

    private final AuditService auditService;
    private final AssociationDAO associationDAO;
    private final Jdbi jdbi;
    private final Logger logger;
    private final DataSetDAO dataSetDAO;

    private ConsentDAO consentDAO;
    private ElectionDAO electionDAO;
    private VoteDAO voteDAO;
    private DacService dacService;
    private DataAccessRequestDAO dataAccessRequestDAO;

    @Inject
    public ConsentService(ConsentDAO consentDAO, ElectionDAO electionDAO, VoteDAO voteDAO, DacService dacService,
                          DataAccessRequestDAO dataAccessRequestDAO, AuditService auditService, AssociationDAO associationDAO, Jdbi jdbi, DataSetDAO dataSetDAO) {
        this.consentDAO = consentDAO;
        this.electionDAO = electionDAO;
        this.voteDAO = voteDAO;
        this.dacService = dacService;
        this.dataAccessRequestDAO = dataAccessRequestDAO;
        this.auditService = auditService;
        this.associationDAO = associationDAO;
        this.jdbi = jdbi;
        this.dataSetDAO = dataSetDAO;
        this.logger = LoggerFactory.getLogger(this.getClass());
    }

    public Consent getById(String id) throws UnknownIdentifierException {
        Consent consent = consentDAO.findConsentById(id);
        if (consent == null) {
            throw new UnknownIdentifierException(String.format("Could not find consent with id %s", id));
        }
        Election election = electionDAO.findLastElectionByReferenceIdAndType(id, ElectionType.TRANSLATE_DUL.getValue());
        if (election != null) {
            consent.setLastElectionStatus(election.getStatus());
            consent.setLastElectionArchived(election.getArchived());
        }
        return consent;
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
        if (StringUtils.isNotEmpty(rec.consentId) && consentDAO.checkConsentById(rec.consentId) != null) {
            throw new IllegalArgumentException("Consent for the specified id already exist");
        }
        Date createDate = new Date();
        consentDAO.insertConsent(id, rec.getRequiresManualReview(),
                rec.getUseRestriction().toString(), rec.getDataUse().toString(),
                rec.getDataUseLetter(), rec.getName(), rec.getDulName(), createDate, createDate,
                rec.getTranslatedUseRestriction(), rec.getGroupName(),
                rec.getDacId());
        return consentDAO.findConsentById(id);
    }

    // POST /consent/:consentid/association <body>=List<ConsentAssociation>
    // Create new associations for a consent.  For each ConsentAssociation specified, remove the previous
    // association and create the new one.
    public List<ConsentAssociation> createAssociation(String consentId, List<ConsentAssociation> new_associations, String createdByUserEmail) {
        logger.trace(String.format("createAssociation consentId='%s' %d associations supplied",
                consentId, new_associations.size()));
        checkConsentExists(consentId);
        validateAssociations(new_associations);
        for (ConsentAssociation association : new_associations) {
            logger.debug(String.format("CreateAssociation, adding associations for '%s', %d ids supplied",
                    association.getAssociationType(), association.getElements().size()));
            validateEmptyObjectIds(association.getElements());
            processAssociation(association.getElements());
            try {
                consentDAO.deleteAllAssociationsForType(consentId, association.getAssociationType());
                List<String> generatedIds = updateAssociations(consentId, association.getAssociationType(), association.getElements());
                auditService.saveAssociationAuditList(generatedIds, AuditTable.CONSENT_ASSOCIATIONS.getValue(), Actions.CREATE.getValue(), createdByUserEmail);
            } catch (Exception e) {
                throw new IllegalArgumentException("Please verify element ids, some or all of them already exist");
            }
        }
        return getAllAssociationsForConsent(consentId);
    }

    public void updateConsentDac(String consentId, Integer dacId) {
        consentDAO.updateConsentDac(consentId, dacId);
    }

    public Consent update(String id, Consent rec) throws NotFoundException {
        rec = updateConsentDates(rec);
        if (StringUtils.isEmpty(consentDAO.checkConsentById(id))) {
            throw new NotFoundException();
        }
        consentDAO.updateConsent(id, rec.getRequiresManualReview(),
                rec.getUseRestriction().toString(), rec.getDataUse().toString(),
                rec.getDataUseLetter(), rec.getName(), rec.getDulName(), rec.getLastUpdate(),
                rec.getSortDate(), rec.getTranslatedUseRestriction(), rec.getGroupName(), true,
                rec.getDacId());
        return consentDAO.findConsentById(id);
    }

    // PUT /consent/:consentid/association <body>=List<ConsentAssociaiton>
    // Update associations for a consent by adding new associations.  For each ConsentAssociation specified, all
    // the objects specified are added as consent associations.
    public List<ConsentAssociation> updateAssociation(String consentId, List<ConsentAssociation> new_associations, String modifiedByUserEmail) {
        logger.trace(String.format("updateAssociation consentId='%s' associations(%d)= '%s'",
                consentId, new_associations.size(), new_associations.toString()));
        checkConsentExists(consentId);
        validateAssociations(new_associations);
        // Loop over all the ConsentAssociations sent in the body.
        for (ConsentAssociation association : new_associations) {
            String atype = association.getAssociationType();
            List<String> new_ids = association.getElements();
            validateEmptyObjectIds(new_ids);
            logger.debug(String.format("updateAssociation, adding associations for '%s', ids(cnt=%d) are '%s'",
                    atype, new_ids.size(), new_ids.toString()));
            // First retrieve the existing associations for that type.
            List<String> old_ids = consentDAO.findAssociationsByType(consentId, atype);
            // Remove any objectId's that already exist
            new_ids.removeAll(old_ids);
            // and add the new associations
            try {
                if (new_ids.size() > 0) {
                    processAssociation(new_ids);
                    List<String> ids = updateAssociations(consentId, association.getAssociationType(), new_ids);
                    auditService.saveAssociationAuditList(ids, AuditTable.CONSENT_ASSOCIATIONS.getValue(), Actions.REPLACE.getValue(), modifiedByUserEmail);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Please verify element ids, some or all of them already exist");
            }

        }
        return getAllAssociationsForConsent(consentId);
    }

    public List<ConsentManage> describeConsentManage(AuthUser authUser) {
        List<ConsentManage> consentManageList = new ArrayList<>();
        consentManageList.addAll(collectUnreviewedConsents(consentDAO.findUnreviewedConsents()));
        consentManageList.addAll(consentDAO.findConsentManageByStatus(ElectionStatus.OPEN.getValue()));
        consentManageList.addAll(consentDAO.findConsentManageByStatus(ElectionStatus.CANCELED.getValue()));
        List<ConsentManage> closedElections = consentDAO.findConsentManageByStatus(ElectionStatus.CLOSED.getValue());
        closedElections.forEach(consentManage -> {
            Boolean vote = voteDAO.findChairPersonVoteByElectionId(consentManage.getElectionId());
            consentManage.setVote(vote != null && vote ? "Approved" : "Denied");
        });
        consentManageList.addAll(closedElections);
        consentManageList.sort((c1, c2) -> c2.getSortDate().compareTo(c1.getSortDate()));
        List<Election> openElections = electionDAO.findElectionsWithFinalVoteByTypeAndStatus(ElectionType.DATA_ACCESS.getValue(), ElectionStatus.OPEN.getValue());
        if (!openElections.isEmpty()) {
            List<String> referenceIds = openElections.stream().map(Election::getReferenceId).collect(Collectors.toList());
            List<DataAccessRequest> dataAccessRequests = dataAccessRequestDAO.findByReferenceIds(referenceIds);
            List<String> datasetIds =
                dataAccessRequests.stream()
                    .filter(Objects::nonNull)
                    .map(DataAccessRequest::getData)
                    .filter(Objects::nonNull)
                    .map(DataAccessRequestData::getDatasetDetail)
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .map(DatasetDetailEntry::getDatasetId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            List<String> consentIds = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(datasetIds)) {
                List<Integer> datasetIdIntValues = datasetIds.stream().map(Integer::valueOf).collect(Collectors.toList());
                consentIds.addAll(consentDAO.getAssociationConsentIdsFromDatasetIds(datasetIdIntValues));
            }

            for (ConsentManage consentManage : consentManageList) {
                if (consentIds.stream().anyMatch(cm -> cm.equals(consentManage.getConsentId()))) {
                    consentManage.setEditable(false);
                } else {
                    consentManage.setEditable(true);
                }
            }
        }
        return dacService.filterConsentManageByDAC(consentManageList, authUser);
    }

    public List<ConsentAssociation> getAssociation(String consentId, String associationType, String objectId) {
        logger.trace(String.format("getAssociation consentId='%s' associationType='%s', objectId='%s'",
                consentId, associationType, objectId));

        checkConsentExists(consentId);
        List<ConsentAssociation> result;
        if (associationType == null)
            result = getAllAssociationsForConsent(consentId);
        else {
            result = new ArrayList<>();
            List<String> id_list;
            if (objectId == null)
                id_list = consentDAO.findAssociationsByType(consentId, associationType);
            else { // both associationType and objectId specified
                id_list = new ArrayList<>();
                if (consentDAO.findAssociationByTypeAndId(consentId, associationType, objectId) != null)
                    id_list.add(objectId);
            }
            result.add(new ConsentAssociation(associationType, id_list));
        }
        return result;
    }

    public Consent retrieve(String id) throws UnknownIdentifierException {
        Consent consent = consentDAO.findConsentById(id);
        if (consent == null) {
            throw new UnknownIdentifierException(String.format("Could not find consent with id %s", id));
        }

        Election election = electionDAO.findLastElectionByReferenceIdAndType(id, ElectionType.TRANSLATE_DUL.getValue());
        if (election != null) {
            consent.setLastElectionStatus(election.getStatus());
            consent.setLastElectionArchived(election.getArchived());
        }
        return consent;
    }

    public Consent getConsentFromDatasetID(Integer datasetId) {
        return consentDAO.findConsentFromDatasetID(datasetId);
    }

    public Integer getUnReviewedConsents(AuthUser authUser) {
        Collection<Consent> consents = consentDAO.findUnreviewedConsents();
        return dacService.filterConsentsByDAC(consents, authUser).size();
    }

    private List<ConsentManage> collectUnreviewedConsents(List<Consent> consents) {
        String UNREVIEWED = "un-reviewed"; // TODO: Fix this in https://broadinstitute.atlassian.net/browse/DUOS-469
        List<ConsentManage> consentManageList = consents.stream().map(ConsentManage::new).collect(Collectors.toList());
        consentManageList.forEach(c -> c.setElectionStatus(UNREVIEWED));
        consentManageList.forEach(c -> c.setEditable(true));
        return consentManageList;
    }

    public void delete(String id) throws IllegalArgumentException {
        checkConsentExists(id);
        List<Election> elections = electionDAO.findElectionsWithFinalVoteByReferenceId(id);
        if (elections.isEmpty()) {
            consentDAO.deleteConsent(id);
            consentDAO.deleteAllAssociationsForConsent(id);
        } else
            throw new IllegalArgumentException("Consent cannot be deleted because already exist elections associated with it");
    }

    public List<ConsentAssociation> deleteAssociation(String consentId, String associationType, String objectId) {
        logger.trace(String.format("deleteAssociation consentId='%s' associationType='%s', objectId='%s'",
                consentId, (associationType == null ? "<null>" : associationType),
                (objectId == null ? "<null>" : objectId)));

        checkConsentExists(consentId);
        if (associationType == null)
            consentDAO.deleteAllAssociationsForConsent(consentId);
        else if (objectId == null)
            consentDAO.deleteAllAssociationsForType(consentId, associationType);
        else { // both associationType and objectId specified
            if (consentDAO.findAssociationByTypeAndId(consentId, associationType, objectId) == null){
                throw new NotFoundException();
            } else {
                Integer datasetId = dataSetDAO.findDataSetIdByObjectId(objectId);
                consentDAO.deleteOneAssociation(consentId, associationType, datasetId);
            }

        }
        return getAllAssociationsForConsent(consentId);
    }

    public Consent updateConsentDul(String consentId, String dataUseLetter, String dulName) throws UnknownIdentifierException {
        Consent consent = retrieve(consentId);
        consent.setDulName(dulName);
        consent.setDataUseLetter(dataUseLetter);
        update(consentId, updateConsentDates(consent));
        return retrieve(consentId);
    }

    public String getConsentDulUrl(String consentId) throws UnknownIdentifierException {
        Consent consent = retrieve(consentId);
        return consent.getDataUseLetter();
    }

    /**
     * Can't add this to the DAO interface =(
     **/
    private List<String> updateAssociations(String consentId, String associationType, List<String> ids) {
        Handle h = jdbi.open();
        PreparedBatch insertBatch = h.prepareBatch("insert into consentassociations (consentId, associationType, dataSetId) values (?, ?, ?)");
        for (String id : ids) {
            insertBatch.add(consentId, associationType, dataSetDAO.findDataSetIdByObjectId(id));
        }
        List<Long> insertedIds = insertBatch.
                executeAndReturnGeneratedKeys("associationid").
                collectInto(new GenericType<List<Long>>() {});
        h.close();
        List<String> stringsList = new ArrayList<>();
        for (Long id : insertedIds) stringsList.add(id.toString());
        return stringsList;
    }

    private void validateEmptyObjectIds(List<String> newIds) {
        if (CollectionUtils.isNotEmpty(newIds)) {
            newIds.stream().forEach(objectId -> {
                if (StringUtils.isEmpty(objectId)) {
                    throw new IllegalArgumentException("Elements are required");
                }
            });
        }
    }

    private void processAssociation(List<String> objectIds) {
        if (CollectionUtils.isNotEmpty(objectIds)) {
            List<DataSet> dataSets = dataSetDAO.getDataSetsForObjectIdList(objectIds);
            List<String> existentObjectsId = dataSets.stream().map(DataSet::getObjectId).collect(Collectors.toList());
            List<DataSet> dataSetsToCreate = new ArrayList<>();
            if(CollectionUtils.isNotEmpty(dataSets)) {
                objectIds.stream().forEach(objectId -> {
                    if(!existentObjectsId.contains(objectId)) {
                        dataSetsToCreate.add(new DataSet(objectId));
                    }
                });
            } else {
                objectIds.stream().forEach(objectId -> {
                    dataSetsToCreate.add(new DataSet(objectId));
                });
            }
            dataSetDAO.insertAll(dataSetsToCreate);
        }
    }

    // Helper methods for Consent Associations
    //
    // Check that the list of ConsentAssociations given as an agrument is valid, and throw BadRequestException
    // if not.  The only error checked for is duplicate associationType.
    private void validateAssociations(List<ConsentAssociation> assoc_list) {
        if (assoc_list.size() > 1) {
            Set<String> atype_list = new HashSet<>();
            for (ConsentAssociation assoc : assoc_list) {
                if (atype_list.contains(assoc.getAssociationType()))
                    throw new WebApplicationException(Response.Status.BAD_REQUEST);
                atype_list.add(assoc.getAssociationType());
            }
        }
    }

    // Check that the specified Consent resource exists, or throw NotFoundException.
    private void checkConsentExists(String consentId) {
        String ck_id = consentDAO.checkConsentById(consentId);
        logger.debug(String.format("CreateAssocition, checkConsentbyId returned '%s'", (ck_id == null ? "<null>" : ck_id)));
        if (ck_id == null)
            throw new NotFoundException(String.format("Consent with id '%s' not found", consentId));
    }

    // Get the updated list of all the associations for a Consent Resource, to return as a result.
    private List<ConsentAssociation> getAllAssociationsForConsent(String consentId) {
        List<ConsentAssociation> assoc_list = new ArrayList<>();
        List<String> type_list = consentDAO.findAssociationTypesForConsent(consentId);
        logger.debug(String.format("getAllAssociationsForConsent consentId='%s', types='%s'", consentId, type_list.toString()));
        for (String atype : type_list) {
            List<String> id_list = consentDAO.findAssociationsByType(consentId, atype);
            logger.debug(String.format("getAllAssociationsForConsent adding %d ids to type '%s'", id_list.size(), atype));
            ConsentAssociation next_assoc = new ConsentAssociation(atype, id_list);
            assoc_list.add(next_assoc);
        }
        logger.debug(String.format("getAllAssociationsForConsent - returning '%s'", assoc_list.toString()));
        return assoc_list;
    }

    public boolean hasWorkspaceAssociation(String workspaceId) {
        return !Objects.isNull(associationDAO.findAssociationIdByTypeAndObjectId(AssociationType.WORKSPACE.getValue(), workspaceId));
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
