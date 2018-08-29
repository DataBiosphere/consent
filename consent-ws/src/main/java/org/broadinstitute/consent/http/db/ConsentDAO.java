package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ConsentDataSet;
import org.broadinstitute.consent.http.models.ConsentManage;
import org.broadinstitute.consent.http.models.dto.UseRestrictionDTO;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.unstable.BindIn;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

@UseStringTemplate3StatementLocator
@RegisterMapper({ConsentMapper.class})
public interface ConsentDAO extends Transactional<ConsentDAO> {

    @SqlQuery("select * from consents where consentId = :consentId and active=true")
    Consent findConsentById(@Bind("consentId") String consentId);

    @SqlQuery("SELECT c.* " +
            "FROM consents c INNER JOIN consentassociations cs ON c.consentId = cs.consentId "+
            "WHERE cs.dataSetId = :datasetId")
    Consent findConsentFromDatasetID(@Bind("datasetId") Integer datasetId);

    @SqlQuery("SELECT c.name " +
            "FROM consents c INNER JOIN consentassociations cs ON c.consentId = cs.consentId "+
            "WHERE cs.dataSetId = :datasetId")
    String findConsentNameFromDatasetID(@Bind("datasetId") String datasetId);

    @SqlQuery("select * from consents  where consentId in (<consentIds>)")
    Collection<Consent> findConsentsFromConsentsIDs(@BindIn("consentIds") List<String> consentIds);

    @SqlQuery("select consentId from consents  where name in (<names>)")
    List<String> findConsentsIdFromConsentNames(@BindIn("names") List<String> names);

    @Mapper(ConsentDataSetMapper.class)
    @SqlQuery("SELECT c.consentId, cs.dataSetId, ds.name " +
            "FROM consents c INNER JOIN consentassociations cs ON c.consentId = cs.consentId " +
            "INNER JOIN dataset ds on cs.dataSetId = ds.dataSetId "+
            "WHERE cs.dataSetId IN (<datasetId>)")
    Set<ConsentDataSet> getConsentIdAndDataSets(@BindIn("datasetId") List<String> datasetId);

    @SqlQuery("select consentId from consents where consentId = :consentId and active=true")
    String checkConsentbyId(@Bind("consentId") String consentId);

    @SqlQuery("select consentId from consents where name = :name")
    String getIdByName(@Bind("name") String name);

    @SqlQuery("select * from consents where name = :name and active=true")
    Consent findConsentByName(@Bind("name") String name);

    @SqlQuery("select c.* from consents c inner join consentassociations a on c.consentId = a.consentId where c.active=true and a.associationType = :associationType ")
    Collection<Consent> findConsentsByAssociationType(@Bind("associationType") String associationType);

    @SqlUpdate("insert into consents " +
            "(consentId, requiresManualReview, useRestriction, dataUse, dataUseLetter, active, name, dulName, createDate, sortDate, translatedUseRestriction, valid_restriction, groupName) values " +
            "(:consentId, :requiresManualReview, :useRestriction, :dataUse, :dataUseLetter, true, :name , :dulName, :createDate, :sortDate , :translatedUseRestriction, :valid_restriction, :groupName)")
    void insertConsent(@Bind("consentId") String consentId,
                       @Bind("requiresManualReview") Boolean requiresManualReview,
                       @Bind("useRestriction") String useRestriction,
                       @Bind("dataUse") String dataUse,
                       @Bind("dataUseLetter") String dataUseLetter,
                       @Bind("name") String name,
                       @Bind("dulName") String dulName,
                       @Bind("createDate") Date createDate,
                       @Bind("sortDate") Date sortDate,
                       @Bind("translatedUseRestriction") String translatedUseRestriction,
                       @Bind("valid_restriction") Boolean validRestriction,
                       @Bind("groupName") String groupName);

    @SqlUpdate("delete from consents where consentId = :consentId")
    void deleteConsent(@Bind("consentId") String consentId);


    @SqlUpdate("update consents set active=false where consentId = :consentId")
    void logicalDeleteConsent(@Bind("consentId") String consentId);


    @SqlUpdate("update consents set requiresManualReview = :requiresManualReview, " +
            "useRestriction = :useRestriction, dataUse = :dataUse, dataUseLetter = :dataUseLetter, name = :name, " +
            "dulName = :dulName, " +
            "lastUpdate = :lastUpdate, sortDate = :sortDate, translatedUseRestriction = :translatedUseRestriction, " +
            "groupName = :groupName, updated = :updated where consentId = :consentId and active = true")
    void updateConsent(@Bind("consentId") String consentId,
                       @Bind("requiresManualReview") Boolean requiresManualReview,
                       @Bind("useRestriction") String useRestriction,
                       @Bind("dataUse") String dataUse,
                       @Bind("dataUseLetter") String dataUseLetter,
                       @Bind("name") String name,
                       @Bind("dulName") String dulName,
                       @Bind("lastUpdate") Date createDate,
                       @Bind("sortDate") Date sortDate,
                       @Bind("translatedUseRestriction") String translatedUseRestriction,
                       @Bind("groupName") String groupName,
                       @Bind("updated")  Boolean updateStatus);

    @SqlUpdate("update consents set sortDate = :sortDate " +
            "where consentId = :consentId and active = true")
    void updateConsentSortDate(@Bind("consentId") String consentId, @Bind("sortDate") Date sortDate);

    @SqlUpdate("update consents set lastUpdate = :lastUpdate, sortDate = :sortDate where consentId in (<consentId>) ")
    void bulkUpdateConsentSortDate(@BindIn("consentId") List<String> consentId,
                                   @Bind("lastUpdate") Date lastUpdate,
                                   @Bind("sortDate") Date sortDate);

    // Consent Association Access Methods

    @SqlUpdate("insert into consentassociations (consentId, associationType, dataSetId) values (:consentId, :associationType, :dataSetId)")
    void insertConsentAssociation(@Bind("consentId") String consentId,
                       @Bind("associationType") String associationType,
                       @Bind("dataSetId") Integer dataSetId);


    @SqlQuery("select ds.objectId from consentassociations ca inner join dataset ds on ds.dataSetId = ca.dataSetId where ca.consentId = :consentId and ca.associationType = :associationType")
    List<String> findAssociationsByType(@Bind("consentId") String consentId,
                                        @Bind("associationType") String associationType);

    @SqlQuery("select associationId from consentassociations where dataSetId = :datasetId")
    Integer findAssociationsByDataSetId(@Bind("datasetId") Integer datasetId);

    @SqlQuery("select  ds.objectId from consentassociations ca inner join dataset ds on ds.dataSetId = ca.dataSetId where ca.consentId = :consentId and ca.associationType = :associationType and ds.objectId = :objectId")
    String findAssociationByTypeAndId(@Bind("consentId") String consentId,
                                      @Bind("associationType") String associationType,
                                      @Bind("objectId") String objectId);

    @SqlUpdate("delete ca from consentassociations ca inner join dataset ds on ds.dataSetId = ca.dataSetId where ca.consentId = :consentId and ca.associationType = :associationType and ds.objectId = :objectId")
    void deleteOneAssociation(@Bind("consentId") String consentId,
                              @Bind("associationType") String associationType,
                              @Bind("objectId") String objectId);

    @SqlUpdate("delete from consentassociations where consentId = :consentId and associationType = :associationType")
    void deleteAllAssociationsForType(@Bind("consentId") String consentId,
                                      @Bind("associationType") String associationType);

    @SqlUpdate("delete from consentassociations where consentId = :consentId")
    void deleteAllAssociationsForConsent(@Bind("consentId") String consentId);

    @SqlUpdate("delete from consentassociations where dataSetId = :dataSetId")
    void deleteAssociationsByDataSetId(@Bind("dataSetId") Integer dataSetId);

    @SqlQuery("select distinct(associationType) from consentassociations where consentId = :consentId")
    List<String> findAssociationTypesForConsent(@Bind("consentId") String consentId);

    @SqlQuery("select distinct(consentId) from consentassociations ca inner join dataset ds on ds.dataSetId = ca.dataSetId where ca.associationType = :associationType and ds.objectId= :objectId")
    List<String> findConsentsForAssociation(@Bind("associationType") String associationType,
                                            @Bind("objectId") String objectId);

    @SqlQuery("select c.* from consentassociations ca inner join consents c on c.consentId = ca.consentId inner join dataset ds on ca.dataSetId = ds.dataSetId where ca.associationType = :associationType and ds.objectId= :objectId")
    Consent findConsentByAssociationAndObjectId(@Bind("associationType") String associationType,
                                            @Bind("objectId") String objectId);

    @SqlQuery("select * from consents where consentId not in (select c.consentId from consents c  inner join election e on e.referenceId = c.consentId )")
    List<Consent> findUnreviewedConsents();

    @SqlQuery("select requiresManualReview from consents where consentId = :consentId")
    Boolean checkManualReview(@Bind("consentId") String consentId);

    @SqlQuery("select c.consentId, c.name, c.createDate, c.sortDate, c.groupName, c.updated, e.electionId, e.status, e.version, e.archived  " +
            "from consents c inner join election e ON e.referenceId = c.consentId inner join ( "+
            "select referenceId, MAX(createDate) maxDate from election e group by referenceId) electionView "+
            "ON electionView.maxDate = e.createDate AND electionView.referenceId = e.referenceId AND e.status = :status")
    @Mapper(ConsentManageMapper.class)
    List<ConsentManage> findConsentManageByStatus(@Bind("status") String status);

    @SqlQuery("select ca.consentId from consentassociations ca  where ca.dataSetId IN (<dataSetIdList>) ")
    List<String> getAssociationsConsentIdfromDataSetIds(@BindIn("dataSetIdList") List<String> dataSetIdList);

    @Mapper(UseRestrictionMapper.class)
    @SqlQuery("select consentId, name, useRestriction from consents where valid_restriction = false ")
    List<UseRestrictionDTO> findInvalidRestrictions();

    @Mapper(UseRestrictionMapper.class)
    @SqlQuery("select consentId, useRestriction, name from consents ")
    List<UseRestrictionDTO> findConsentUseRestrictions();

    @SqlUpdate("update consents set  valid_restriction = :valid_restriction where consentId in (<consentId>) ")
    void updateConsentValidUseRestriction(@BindIn("consentId") List<String> consentId,
                                   @Bind("valid_restriction") Boolean validRestriction);

    @SqlUpdate("update consents set updated = :consentStatus where consentId = :referenceId")
    void updateConsentUpdateStatus(@Bind("referenceId") String referenceId,
                                   @Bind("consentStatus") Boolean consentStatus);

    @SqlUpdate("update consents set groupName = :groupName where consentId = :consentId")
    void updateConsentGroupName(@Bind("consentId") String consentId,
                                @Bind("groupName") String groupName);

}
