package org.broadinstitute.consent.http.models;

import java.util.Date;

public class WorkspaceAudit {

    private String modifiedObjectId;

    private String modifiedTable;

    private String changeAction;

    private long modifiedByUserId;

    private Date modificationDate;

    public WorkspaceAudit(String modifiedObjectId, String modifiedTable, String changeAction, int modifiedByUserId, Date modificationDate) {
        this.modifiedObjectId = modifiedObjectId;
        this.modifiedTable = modifiedTable;
        this.changeAction = changeAction;
        this.modifiedByUserId = modifiedByUserId;
        this.modificationDate = modificationDate;
    }

    public String getModifiedObjectId() {
        return modifiedObjectId;
    }

    public String getModifiedTable() {
        return modifiedTable;
    }

    public String getChangeAction() {
        return changeAction;
    }

    public long getModifiedByUserId() {
        return modifiedByUserId;
    }

    public Date getModificationDate() {
        return modificationDate;
    }
}
