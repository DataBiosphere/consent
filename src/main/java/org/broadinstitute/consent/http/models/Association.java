package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Association Representation object.
 * This class represents a single row of consentassociation data. There can be many `associations`
 * to a single consent which would indicate multiple datasets using the same consent.
 * See also: ConsentAssociation
 */
public class Association {

    @JsonProperty
    public Integer associationId;

    @JsonProperty
    public String consentId;

    @JsonProperty
    public String associationType;

    @JsonProperty
    public String objectId;

    @JsonProperty
    public Integer dataSetId;

    public Integer getAssociationId() {
        return associationId;
    }

    public void setAssociationId(Integer associationId) {
        this.associationId = associationId;
    }

    public String getConsentId() {
        return consentId;
    }

    public void setConsentId(String consentId) {
        this.consentId = consentId;
    }

    public String getAssociationType() {
        return associationType;
    }

    public void setAssociationType(String associationType) {
        this.associationType = associationType;
    }

    public String getObjectId() {
        return objectId;
    }

    public Integer getDataSetId() {
        return dataSetId;
    }

    public void setDataSetId(Integer dataSetId) {
        this.dataSetId = dataSetId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }
}
