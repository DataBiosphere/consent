/*
 * Copyright 2014 Broad Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    public Consent findConsentById(@Bind("consentId") String consentId);

    @SqlQuery("select consentId from consents where consentId = :consentId and active=true")
    public String checkConsentbyId(@Bind("consentId") String consentId);

    @SqlUpdate("insert into consents " +
            "(consentId, requiresManualReview, useRestriction, active) values " +
            "(:consentId, :requiresManualReview, :useRestriction, true)")
    public void insertConsent(@Bind("consentId") String consentId,
                            @Bind("requiresManualReview") Boolean requiresManualReview,
                            @Bind("useRestriction") String useRestriction);

    @SqlUpdate("update consents set active=false where consentId = :consentId")
    public void deleteConsent(@Bind("consentId") String consentId);

    @SqlUpdate("update consents set requiresManualReview = :requiresManualReview, " +
            "useRestriction = :useRestriction where consentId = :consentId and active = true")
    public void updateConsent(@Bind("consentId") String consentId,
                              @Bind("requiresManualReview") Boolean requiresManualReview,
                              @Bind("useRestriction") String useRestriction);

    // Consent Association Access Methods
    @SqlQuery("select objectId from consentassociations where consentId = :consentId and associationType = :associationType")
    public List<String> findAssociationsByType(@Bind("consentId") String consentId,
                                               @Bind("associationType") String associationType);

    @SqlQuery("select objectId from consentassociations where consentId = :consentId and associationType = :associationType and objectId = :objectId")
    public String findAssociationByTypeAndId(@Bind("consentId") String consentId,
                                             @Bind("associationType") String associationType,
                                             @Bind("objectId") String objectId);

    @SqlBatch("insert into consentassociations (consentId, associationType, objectId) values (:consentId, :associationType, :objectId)")
    public void insertAssociations(@Bind("consentId") String consentId,
                                   @Bind("associationType") String associationType,
                                   @Bind("objectId") List<String> ids);

    @SqlBatch("delete from consentassociations where consentId = :consentId and associationType = :associationType and objectId =: objectId")
    public void deleteAssociations(@Bind("consentId") String consentId,
                                   @Bind("associationType") String associationType,
                                   @Bind("objectId") List<String> ids);

    @SqlUpdate("delete from consentassociations where consentId = :consentId and associationType = :associationType and objectId = :objectId")
    public void deleteOneAssociation(@Bind("consentId") String consentId,
                                   @Bind("associationType") String associationType,
                                   @Bind("objectId") String objectId);

    @SqlUpdate("delete from consentassociations where consentId = :consentId and associationType = :associationType")
    public void deleteAllAssociationsForType(@Bind("consentId") String consentId,
                                             @Bind("associationType") String associationType);

    @SqlUpdate("delete from consentassociations where consentId = :consentId")
    public void deleteAllAssociationsForConsent(@Bind("consentId") String consentId);


    @SqlQuery("select distinct(associationType) from consentassociations where consentId = :consentId")
    public List<String> findAssociationTypesForConsent(@Bind("consentId") String consentId);

    @SqlQuery("select distinct(consentId) from consentassociations where associationType = :associationType and objectId= :objectId")
    public List<String> findConsentsForAssociation(@Bind("associationType") String associationType,
                                                   @Bind("objectId") String objectId);

}
