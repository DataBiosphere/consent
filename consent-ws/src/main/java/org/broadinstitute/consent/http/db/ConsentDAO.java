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
            "WHERE cs.objectId = :datasetId")
    Consent findConsentFromDatasetID(@Bind("datasetId") String datasetId);


    @SqlQuery("select * from consents  where consentId in (<consentIds>)")
    Collection<Consent> findConsentsFromConsentsIDs(@BindIn("consentIds") List<String> consentIds);

    @Mapper(ConsentDataSetMapper.class)
    @SqlQuery("SELECT c.consentId, cs.objectId, ds.name " +
            "FROM consents c INNER JOIN consentassociations cs ON c.consentId = cs.consentId " +
            "INNER JOIN dataset ds on cs.objectId = ds.objectId "+
            "WHERE cs.objectId IN (<datasetId>)")
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
            "(consentId, requiresManualReview, useRestriction, dataUse, dataUseLetter, active, name, dulName, createDate, sortDate, translatedUseRestriction, valid_restriction) values " +
            "(:consentId, :requiresManualReview, :useRestriction, :dataUse, :dataUseLetter, true, :name , :dulName, :createDate, :sortDate , :translatedUseRestriction, :valid_restriction)")
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
                       @Bind("valid_restriction") Boolean validRestriction);



    @SqlUpdate("delete from consents where consentId = :consentId")
    void deleteConsent(@Bind("consentId") String consentId);


    @SqlUpdate("update consents set active=false where consentId = :consentId")
    void logicalDeleteConsent(@Bind("consentId") String consentId);


    @SqlUpdate("update consents set requiresManualReview = :requiresManualReview, " +
            "useRestriction = :useRestriction, dataUse = :dataUse, dataUseLetter = :dataUseLetter, name = :name, " +
            "dulName = :dulName, " +
            "lastUpdate = :lastUpdate, sortDate = :sortDate, translatedUseRestriction = :translatedUseRestriction " +
            "where consentId = :consentId and active = true")
    void updateConsent(@Bind("consentId") String consentId,
                       @Bind("requiresManualReview") Boolean requiresManualReview,
                       @Bind("useRestriction") String useRestriction,
                       @Bind("dataUse") String dataUse,
                       @Bind("dataUseLetter") String dataUseLetter,
                       @Bind("name") String name,
                       @Bind("dulName") String dulName,
                       @Bind("lastUpdate") Date createDate,
                       @Bind("sortDate") Date sortDate,
                       @Bind("translatedUseRestriction") String translatedUseRestriction);

    @SqlUpdate("update consents set sortDate = :sortDate " +
            "where consentId = :consentId and active = true")
    void updateConsentSortDate(@Bind("consentId") String consentId, @Bind("sortDate") Date sortDate);

    @SqlUpdate("update consents set lastUpdate = :lastUpdate, sortDate = :sortDate where consentId in (<consentId>) ")
    void bulkUpdateConsentSortDate(@BindIn("consentId") List<String> consentId,
                                   @Bind("lastUpdate") Date lastUpdate,
                                   @Bind("sortDate") Date sortDate);

    // Consent Association Access Methods
    @SqlQuery("select objectId from consentassociations where consentId = :consentId and associationType = :associationType")
    List<String> findAssociationsByType(@Bind("consentId") String consentId,
                                        @Bind("associationType") String associationType);

    @SqlQuery("select objectId from consentassociations where consentId = :consentId and associationType = :associationType and objectId = :objectId")
    String findAssociationByTypeAndId(@Bind("consentId") String consentId,
                                      @Bind("associationType") String associationType,
                                      @Bind("objectId") String objectId);

    @SqlUpdate("delete from consentassociations where consentId = :consentId and associationType = :associationType and objectId = :objectId")
    void deleteOneAssociation(@Bind("consentId") String consentId,
                              @Bind("associationType") String associationType,
                              @Bind("objectId") String objectId);

    @SqlUpdate("delete from consentassociations where consentId = :consentId and associationType = :associationType")
    void deleteAllAssociationsForType(@Bind("consentId") String consentId,
                                      @Bind("associationType") String associationType);

    @SqlUpdate("delete from consentassociations where consentId = :consentId")
    void deleteAllAssociationsForConsent(@Bind("consentId") String consentId);


    @SqlQuery("select distinct(associationType) from consentassociations where consentId = :consentId")
    List<String> findAssociationTypesForConsent(@Bind("consentId") String consentId);

    @SqlQuery("select distinct(consentId) from consentassociations where associationType = :associationType and objectId= :objectId")
    List<String> findConsentsForAssociation(@Bind("associationType") String associationType,
                                            @Bind("objectId") String objectId);

    @SqlQuery("select c.* from consentassociations ca inner join consents c on c.consentId = ca.consentId where associationType = :associationType and objectId= :objectId")
    Consent findConsentByAssociationAndObjectId(@Bind("associationType") String associationType,
                                            @Bind("objectId") String objectId);

    @SqlQuery("select * from consents where consentId not in (select c.consentId from consents c  inner join election e on e.referenceId = c.consentId )")
    List<Consent> findUnreviewedConsents();

    @SqlQuery("select requiresManualReview from consents where consentId = :consentId")
    Boolean checkManualReview(@Bind("consentId") String consentId);

    @SqlQuery("select c.consentId, c.name, c.createDate, c.sortDate, e.electionId, e.status " +
            "from consents c inner join election e ON e.referenceId = c.consentId inner join ( "+
            "select referenceId, MAX(createDate) maxDate from election e group by referenceId) electionView "+
            "ON electionView.maxDate = e.createDate AND electionView.referenceId = e.referenceId AND e.status = :status")
    @Mapper(ConsentManageMapper.class)
    List<ConsentManage> findConsentManageByStatus(@Bind("status") String status);

    @SqlQuery("select ca.consentId from consentassociations ca  where ca.objectId IN (<objectIdList>) ")
    List<String> getAssociationsConsentIdfromObjectIds(@BindIn("objectIdList") List<String> objectIdList);

    @Mapper(UseRestrictionMapper.class)
    @SqlQuery("select consentId, name, useRestriction from consents where valid_restriction = false ")
    List<UseRestrictionDTO> findInvalidRestrictions();

    @Mapper(UseRestrictionMapper.class)
    @SqlQuery("select consentId, useRestriction, name from consents ")
    List<UseRestrictionDTO> findConsentUseRestrictions();

    @SqlUpdate("update consents set  valid_restriction = :valid_restriction where consentId in (<consentId>) ")
    void updateConsentValidUseRestriction(@BindIn("consentId") List<String> consentId,
                                   @Bind("valid_restriction") Boolean validRestriction);

}
