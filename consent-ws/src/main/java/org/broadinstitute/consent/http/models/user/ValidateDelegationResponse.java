package org.broadinstitute.consent.http.models.user;

import java.util.List;
import org.broadinstitute.consent.http.models.DACUser;

public class ValidateDelegationResponse {

    private boolean needsDelegation;
    private List<DACUser> delegateCandidates;

    public ValidateDelegationResponse(boolean needsDelegation, List<DACUser> delegateCandidates) {
        this.needsDelegation = needsDelegation;
        this.delegateCandidates = delegateCandidates;
    }

    public ValidateDelegationResponse(List<DACUser> delegateCandidates) {
        this.delegateCandidates = delegateCandidates;
    }

    public boolean isNeedsDelegation() {
        return needsDelegation;
    }

    public void setNeedsDelegation(boolean needsDelegation) {
        this.needsDelegation = needsDelegation;
    }

    public List<DACUser> getDelegateCandidates() {
        return delegateCandidates;
    }

    public void setDelegateCandidates(List<DACUser> delegateCandidates) {
        this.delegateCandidates = delegateCandidates;
    }
}
