package org.broadinstitute.consent.http.models.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.broadinstitute.consent.http.models.Consent;
import org.bson.Document;

import java.util.List;

public class WorkspaceAssociationDTO {

    @JsonProperty
    Consent consent;

    @JsonProperty
    List<Document> dataAccessRequests;

    @JsonProperty
    List<ElectionStatusDTO> electionStatus;

    public WorkspaceAssociationDTO() {
    }

    public WorkspaceAssociationDTO(Consent consent, List<ElectionStatusDTO> electionStatus) {
        this.consent = consent;
        this.electionStatus = electionStatus;
    }

    public Consent getConsent() {
        return consent;
    }

    public void setConsent(Consent consent) {
        this.consent = consent;
    }

    public List<Document> getDataAccessRequests() {
        return dataAccessRequests;
    }

    public void setDataAccessRequests(List<Document> dataAccessRequests) {
        this.dataAccessRequests = dataAccessRequests;
    }

    public List<ElectionStatusDTO> getElectionStatus() {
        return electionStatus;
    }

    public void setElectionStatus(List<ElectionStatusDTO> electionStatus) {
        this.electionStatus = electionStatus;
    }
}
