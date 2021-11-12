package org.broadinstitute.consent.http.models;

import java.util.Date;
import org.apache.commons.lang3.builder.EqualsBuilder;

public class DatasetProperty {

    private Integer propertyId;
    private Integer datasetId;
    private Integer propertyKey;
    private String propertyValue;
    private Date createDate;

    public DatasetProperty(){
    }

    public DatasetProperty(Integer propertyId, Integer datasetId, Integer propertyKey, String propertyValue,
                           Date createDate) {
        this(datasetId, propertyKey, propertyValue, createDate);
        this.propertyId = propertyId;
    }

    public DatasetProperty(Integer datasetId, Integer propertyKey, String propertyValue,
                           Date createDate){
        this.datasetId = datasetId;
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

    public Integer getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(Integer datasetId) {
        this.datasetId = datasetId;
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

        DatasetProperty other = (DatasetProperty) obj;
        return new EqualsBuilder()
              .append(datasetId, other.datasetId)
              .append(propertyKey, other.propertyKey)
              .append(propertyValue, other.propertyValue)
              .isEquals();
    }
}