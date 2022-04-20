package org.broadinstitute.consent.http.models;

import com.google.common.base.Objects;

import java.util.Date;

public class DatasetProperty {

    private Integer propertyId;
    private Integer dataSetId;
    private Integer propertyKey;
    private String propertyName;
    private String propertyValue;
    private Date createDate;

    public DatasetProperty(){
    }

    public DatasetProperty(Integer propertyId, Integer  dataSetId, Integer propertyKey, String propertyValue,
                           Date createDate) {
        this(dataSetId, propertyKey, propertyValue, createDate);
        this.propertyId = propertyId;
    }

    public DatasetProperty(Integer  dataSetId, Integer propertyKey, String propertyValue,
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

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatasetProperty that = (DatasetProperty) o;
        return Objects.equal(dataSetId, that.dataSetId) && Objects.equal(propertyName, that.propertyName) && Objects.equal(propertyValue, that.propertyValue);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(dataSetId, propertyName, propertyValue);
    }
}