package org.broadinstitute.consent.http.models;

import java.util.Objects;
import org.broadinstitute.consent.http.enumeration.PropertyType;

public class StudyProperty {

  private Integer studyPropertyId;
  private Integer studyId;
  private String key;
  private PropertyType type;
  private Object value;

  public StudyProperty() {
  }

  public StudyProperty(String key, Object value, PropertyType type) {
    this.key = key;
    this.value = value;
    this.type = type;
  }

  public Integer getStudyPropertyId() {
    return studyPropertyId;
  }

  public void setStudyPropertyId(Integer studyPropertyId) {
    this.studyPropertyId = studyPropertyId;
  }

  public Integer getStudyId() {
    return studyId;
  }

  public void setStudyId(Integer studyId) {
    this.studyId = studyId;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public PropertyType getType() {
    return type;
  }

  public void setType(PropertyType type) {
    this.type = type;
  }

  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StudyProperty that = (StudyProperty) o;
    return Objects.equals(studyPropertyId, that.studyPropertyId)
        && Objects.equals(studyId, that.studyId) && Objects.equals(key, that.key)
        && type == that.type && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(studyPropertyId, studyId, key, type, value);
  }
}
