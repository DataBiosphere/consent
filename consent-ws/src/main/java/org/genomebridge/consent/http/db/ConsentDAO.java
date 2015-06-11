package org.genomebridge.consent.http.db;

import org.genomebridge.consent.http.models.Consent;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;

import java.util.List;

@RegisterMapper({ ConsentMapper.class })
public interface ConsentDAO extends Transactional<ConsentDAO> {

    @SqlQuery("select * from consents where consentId = :consentId and active=true")
    Consent findConsentById(@Bind("consentId") String consentId);

    @SqlQuery("select consentId from consents where consentId = :consentId and active=true")
    String checkConsentbyId(@Bind("consentId") String consentId);

    @SqlUpdate("insert into consents " +
            "(consentId, requiresManualReview, useRestriction, active) values " +
            "(:consentId, :requiresManualReview, :useRestriction, true)")
    void insertConsent(@Bind("consentId") String consentId,
                            @Bind("requiresManualReview") Boolean requiresManualReview,
                            @Bind("useRestriction") String useRestriction);

    @SqlUpdate("update consents set active=false where consentId = :consentId")
    void deleteConsent(@Bind("consentId") String consentId);

    @SqlUpdate("update consents set requiresManualReview = :requiresManualReview, " +
            "useRestriction = :useRestriction where consentId = :consentId and active = true")
    void updateConsent(@Bind("consentId") String consentId,
                              @Bind("requiresManualReview") Boolean requiresManualReview,
                              @Bind("useRestriction") String useRestriction);

    // Consent Association Access Methods
    @SqlQuery("select objectId from consentassociations where consentId = :consentId and associationType = :associationType")
    List<String> findAssociationsByType(@Bind("consentId") String consentId,
                                               @Bind("associationType") String associationType);

    @SqlQuery("select objectId from consentassociations where consentId = :consentId and associationType = :associationType and objectId = :objectId")
    String findAssociationByTypeAndId(@Bind("consentId") String consentId,
                                             @Bind("associationType") String associationType,
                                             @Bind("objectId") String objectId);

    @SqlBatch("insert into consentassociations (consentId, associationType, objectId) values (:consentId, :associationType, :objectId)")
    void insertAssociations(@Bind("consentId") String consentId,
                                   @Bind("associationType") String associationType,
                                   @Bind("objectId") List<String> ids);

    @SqlBatch("delete from consentassociations where consentId = :consentId and associationType = :associationType and objectId =: objectId")
    void deleteAssociations(@Bind("consentId") String consentId,
                                   @Bind("associationType") String associationType,
                                   @Bind("objectId") List<String> ids);

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

}
