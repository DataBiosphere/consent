package org.genomebridge.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class DataSetProperty {

    private Integer propertyId;
    private Integer dataSetId;
    private String propertyKey;
    private String propertyValue;
    private Date createDate;


    /* only for Hardcoding
     *
     *
     *
     */
    public DataSetProperty(Integer propertyId,Integer  dataSetId,String propertyKey, String propertyValue,
                   Date createDate) {
        this.propertyId = propertyId;
        this.dataSetId = dataSetId;
        this.propertyKey = propertyKey;
        this.propertyValue = propertyValue;
        this.createDate = createDate;

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

    public String getPropertyKey() {
        return propertyKey;
    }

    public void setPropertyKey(String propertyKey) {
        this.propertyKey = propertyKey;
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }
}