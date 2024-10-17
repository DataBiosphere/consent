package org.broadinstitute.consent.http.models;

import com.google.common.base.Objects;
import java.util.Date;
import org.broadinstitute.consent.http.enumeration.PropertyType;

public class DatasetProperty {

  private Integer propertyId;
  private Integer datasetId;
  private Integer propertyKey;
  private String propertyName;
  private Object propertyValue;
  private Date createDate;
  private String schemaProperty;
  private PropertyType propertyType;

  public DatasetProperty() {
  }

  @Deprecated
  public DatasetProperty(Integer propertyId,
      Integer datasetId,
      Integer propertyKey,
      String propertyValue,
      PropertyType type,
      Date createDate) {
    this(datasetId, propertyKey, propertyValue, type, createDate);
    this.propertyId = propertyId;
  }

  @Deprecated
  public DatasetProperty(Integer datasetId,
      Integer propertyKey,
      String propertyValue,
      PropertyType type,
      Date createDate) {
    this.datasetId = datasetId;
    this.propertyKey = propertyKey;
    this.propertyValue = type.coerce(propertyValue);
    this.propertyType = type;
    this.createDate = createDate;
  }

  public DatasetProperty(Integer propertyId,
      Integer datasetId,
      Integer propertyKey,
      String schemaProperty,
      String propertyValue,
      PropertyType type,
      Date createDate) {
    this(datasetId, propertyKey, schemaProperty, propertyValue, type, createDate);
    this.propertyId = propertyId;
  }

  public DatasetProperty(Integer datasetId,
      Integer propertyKey,
      String schemaProperty,
      String propertyValue,
      PropertyType type,
      Date createDate) {
    this.datasetId = datasetId;
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

  public void setPropertyValueAsString(String propertyValue) {
    this.propertyValue = propertyType.coerce(propertyValue);
  }

  public Date getCreateDate() {
    return createDate;
  }

  public void setCreateDate(Date createDate) {
    this.createDate = createDate;
  }

  public PropertyType getPropertyType() {
    if (java.util.Objects.isNull(this.propertyType)) {
      return PropertyType.String;
    }

    return this.propertyType;
  }

  public String getPropertyTypeAsString() {
    return this.getPropertyType().toString();
  }

  public void setPropertyType(PropertyType propertyType) {
    this.propertyType = propertyType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DatasetProperty that = (DatasetProperty) o;
    return Objects.equal(datasetId, that.datasetId) && Objects.equal(propertyName,
        that.propertyName) && Objects.equal(propertyValue, that.propertyValue);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(datasetId, propertyName, propertyValue);
  }
}