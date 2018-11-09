package org.broadinstitute.consent.http.models;

import java.util.Date;

public class DataSetProperty {

    private Integer propertyId;
    private Integer dataSetId;
    private Integer propertyKey;
    private String propertyValue;
    private Date createDate;

    public DataSetProperty(){
    }

    public DataSetProperty(Integer propertyId, Integer  dataSetId, Integer propertyKey, String propertyValue,
                           Date createDate) {
        this(dataSetId, propertyKey, propertyValue, createDate);
        this.propertyId = propertyId;
    }

    public DataSetProperty(Integer  dataSetId, Integer propertyKey, String propertyValue,
                           Date createDate){
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

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }
}