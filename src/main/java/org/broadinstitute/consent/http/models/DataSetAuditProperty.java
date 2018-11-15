package org.broadinstitute.consent.http.models;

import java.util.Date;

public class DataSetAuditProperty {


    private Integer dataSetAuditId;
    private Integer propertyId;
    private Integer dataSetId;
    private Integer propertyKey;
    private String propertyValue;
    private Date date;

    public DataSetAuditProperty(){
    }

    public DataSetAuditProperty(Integer propertyId, Integer dataSetId, Integer propertyKey, String propertyValue,
                                Date date, Integer dataSetAuditId) {
        this.dataSetId = dataSetId;
        this.propertyKey = propertyKey;
        this.propertyValue = propertyValue;
        this.date = date;
        this.propertyId = propertyId;
        this.dataSetAuditId = dataSetAuditId;
    }


    public Integer getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(Integer propertyId) {
        this.propertyId = propertyId;
    }

    public Integer getDataSetId() {
        return dataSetId;
    }

    public void setDataSetId(Integer dataSetId) {
        this.dataSetId = dataSetId;
    }

    public Integer getPropertyKey() {
        return propertyKey;
    }

    public void setPropertyKey(Integer propertyKey) {
        this.propertyKey = propertyKey;
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Integer getDataSetAuditId() {
        return dataSetAuditId;
    }

    public void setDataSetAuditId(Integer dataSetAuditId) {
        this.dataSetAuditId = dataSetAuditId;
    }
}