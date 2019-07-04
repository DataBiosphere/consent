package org.broadinstitute.consent.http.models.user;

import java.util.List;
import org.broadinstitute.consent.http.models.User;

public class ValidateDelegationResponse {

    private boolean needsDelegation;
    private List<User> delegateCandidates;

    public ValidateDelegationResponse(boolean needsDelegation, List<User> delegateCandidates) {
        this.needsDelegation = needsDelegation;
        this.delegateCandidates = delegateCandidates;
    }

    public boolean isNeedsDelegation() {
        return needsDelegation;
    }

    public List<User> getDelegateCandidates() {
        return delegateCandidates;
    }

}
