package org.broadinstitute.consent.http.models;

import java.util.Date;
import org.apache.commons.lang3.builder.EqualsBuilder;

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

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        DataSetProperty other = (DataSetProperty) obj;
        return new EqualsBuilder()
              .append(dataSetId, other.dataSetId)
              .append(propertyKey, other.propertyKey)
              .append(propertyValue, other.propertyValue)
              .isEquals();
    }
}