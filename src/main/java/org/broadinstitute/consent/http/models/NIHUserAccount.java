package org.broadinstitute.consent.http.models;

import org.broadinstitute.consent.http.enumeration.UserFields;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NIHUserAccount {

    private String linkedNihUsername;

    private ArrayList datasetPermissions;

    private String linkExpireTime;

    private Boolean status;

    public NIHUserAccount() {}

    public NIHUserAccount(String linkedNihUsername, ArrayList datasetPermissions, String linkExpireTime, Boolean status) {
        this.linkedNihUsername = linkedNihUsername;
        this.datasetPermissions = datasetPermissions;
        this.linkExpireTime = linkExpireTime;
        this.status = status;
    }

    public Map<String, String> getNihMap() {
        Map<String, String> nihComponents = new HashMap<>();
        nihComponents.put(UserFields.ERA_STATUS.getValue(), Boolean.TRUE.toString());
        nihComponents.put(UserFields.ERA_EXPIRATION_DATE.getValue(), this.linkExpireTime);
        return nihComponents;
    }

    public String getLinkedNihUsername() {
        return linkedNihUsername;
    }

    public void setLinkedNihUsername(String linkedNihUsername) {
        this.linkedNihUsername = linkedNihUsername;
    }

    public String getLinkExpireTime() {
        return linkExpireTime;
    }

    public void setLinkExpireTime(String linkExpireTime) {
        this.linkExpireTime = linkExpireTime;
    }


    public ArrayList getDatasetPermissions() {
        return datasetPermissions;
    }

    public void setDatasetPermissions(ArrayList datasetPermissions) {
        this.datasetPermissions = datasetPermissions;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

}
