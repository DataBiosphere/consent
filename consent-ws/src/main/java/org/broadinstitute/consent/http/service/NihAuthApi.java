package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.models.ResearcherProperty;

import java.util.List;

public interface NihAuthApi {

    List<ResearcherProperty> authenticateNih(NIHUserAccount nihAccount, Integer userId);

    void deleteNihAccountById(Integer userId);
}
