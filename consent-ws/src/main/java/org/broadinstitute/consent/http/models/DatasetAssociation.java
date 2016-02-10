package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;


public class DatasetAssociation {


    @JsonProperty
    private Integer datasetId;

    @JsonProperty
    private Integer dacuserId;

    @JsonProperty
    private Date createDate;

    public DatasetAssociation(){};

    public DatasetAssociation(Integer datasetId, Integer dacuserId){
        this.datasetId = datasetId;
        this.dacuserId = dacuserId;
        this.createDate = new Date();
    }



    public Integer getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(Integer datasetId) {
        this.datasetId = datasetId;
    }

    public void setDacuserId(Integer dacuserId) {
        this.dacuserId = dacuserId;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Integer getDacuserId() {
        return dacuserId;
    }

    public static List<DatasetAssociation>  createDatasetAssociations(Integer datasetId, Collection<Integer> usersIdList){
        ArrayList<DatasetAssociation> associationList = new ArrayList<>();
        usersIdList.forEach(dacUserId -> associationList.add(new DatasetAssociation(datasetId,dacUserId)));
        return associationList;
    }
}
