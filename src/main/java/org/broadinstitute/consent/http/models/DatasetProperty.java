package org.broadinstitute.consent.http.models;

import com.google.common.base.Objects;
import org.broadinstitute.consent.http.enumeration.DatasetPropertyType;

import java.util.Date;

public class DatasetProperty {

    private Integer propertyId;
    private Integer dataSetId;
    private Integer propertyKey;
    private String propertyName;
    private Object propertyValue;
    private Date createDate;
    private String schemaProperty;
    private DatasetPropertyType propertyType;

    public DatasetProperty(){
    }

    public DatasetProperty(Integer propertyId,
                           Integer  dataSetId,
                           Integer propertyKey,
                           String propertyValue,
                           DatasetPropertyType type,
                           Date createDate) {
        this(dataSetId, propertyKey, propertyValue, type, createDate);
        this.propertyId = propertyId;
    }

    public DatasetProperty(Integer  dataSetId,
                           Integer propertyKey,
                           String propertyValue,
                           DatasetPropertyType type,
                           Date createDate){
        this.dataSetId = dataSetId;
        this.propertyKey = propertyKey;
        this.propertyValue = type.coerce(propertyValue);
        this.propertyType = type;
        this.createDate = createDate;
    }

    public DatasetProperty(Integer propertyId,
                           Integer  dataSetId,
                           Integer propertyKey,
                           String schemaProperty,
                           String propertyValue,
                           DatasetPropertyType type,
                           Date createDate) {
        this(dataSetId, propertyKey, schemaProperty, propertyValue, type, createDate);
        this.propertyId = propertyId;
    }

    public DatasetProperty(Integer  dataSetId,
                           Integer propertyKey,
                           String schemaProperty,
                           String propertyValue,
                           DatasetPropertyType type,
                           Date createDate){
        this.dataSetId = dataSetId;
        this.propertyKey = propertyKey;
        this.propertyValue = type.coerce(propertyValue);
        this.propertyType = type;
        this.createDate = createDate;
        this.schemaProperty = schemaProperty;
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

    public String getSchemaProperty() {
        return this.schemaProperty;
    }

    public void setSchemaProperty(String schemaProperty) {
        this.schemaProperty = schemaProperty;
    }

    public Object getPropertyValue() {
        return propertyValue;
    }

    public String getPropertyValueAsString() {
        return this.propertyValue.toString();
    }

    public void setPropertyValue(Object propertyValue) {
        this.propertyValue = propertyValue;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public DatasetPropertyType getPropertyType() {
        return this.propertyType;
    }

    public void setPropertyType(DatasetPropertyType propertyType) {
        this.propertyType = propertyType;
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