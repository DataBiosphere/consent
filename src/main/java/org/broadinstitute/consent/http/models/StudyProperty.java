package org.broadinstitute.consent.http.models;

import org.broadinstitute.consent.http.enumeration.PropertyType;

public class StudyProperty {
    private Integer studyPropertyId;
    private Integer studyId;
    private String name;
    private PropertyType type;
    private Object value;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
