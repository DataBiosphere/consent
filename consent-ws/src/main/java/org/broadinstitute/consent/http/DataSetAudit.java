package org.broadinstitute.consent.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;

public class DataSetAudit {

    @JsonProperty
    private Integer dataSetAuditId;

    @JsonProperty
    private Integer dataSetId;

    @JsonProperty
    private String objectId;

    @JsonProperty
    private String name;

    @JsonProperty
    private Date date;

    @JsonProperty
    private Boolean active;

    @JsonProperty
    private Integer user;

    @JsonProperty
    private String action;


    public DataSetAudit() {
    }

    public DataSetAudit(Integer dataSetId, String objectId, String name, Date date, Boolean active, Integer user, String action) {
        this.dataSetId = dataSetId;
        this.objectId = objectId;
        this.name = name;
        this.date = date;
        this.active = active;
        this.user = user;
        this.action = action;
    }

    public Integer getDataSetAuditId() {
        return dataSetAuditId;
    }

    public void setDataSetAuditId(Integer dataSetAuditId) {
        this.dataSetAuditId = dataSetAuditId;
    }

    public Integer getDataSetId() {
        return dataSetId;
    }

    public void setDataSetId(Integer dataSetId) {
        this.dataSetId = dataSetId;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getUser() {
        return user;
    }

    public void setUser(Integer user) {
        this.user = user;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

}
