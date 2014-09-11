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

import org.genomebridge.consent.http.resources.ConsentResource;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;

@RegisterMapper({ ConsentResourceMapper.class })
public interface ConsentDAO {

    @SqlQuery("select * from consents where consentId = :consentId and active=true")
    public ConsentResource findConsentById(@Bind("consentId") String consentId);

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

}
