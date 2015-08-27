package org.genomebridge.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.Set;

public class DataSet {

    @JsonProperty
    private Integer dataSetId;

    @JsonProperty
    private String objectId;

    @JsonProperty
    private String name;

    @JsonProperty
    private Date createDate;

    private Set<DataSetProperty> properties;


    public DataSet() {
    }

    public DataSet(Integer dataSetId, String objectId, String name, Date createDate) {
        this.dataSetId = dataSetId;
        this.objectId = objectId;
        this.name = name;
        this.createDate = createDate;
    }

    public Integer getDataSetId() {
        return dataSetId;
    }

    public void setDataSetId(Integer dataSetId) {
        this.dataSetId = dataSetId;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Set<DataSetProperty> getProperties() {
        return properties;
    }

    public void setProperties(Set<DataSetProperty> properties) {
        this.properties = properties;
    }
}
