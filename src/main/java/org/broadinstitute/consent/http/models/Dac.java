package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Entity representing a Data Access Committee
 */
@JsonInclude(Include.NON_NULL)
public class Dac {

    @JsonProperty
    private Integer dacId;

    @JsonProperty
    private String name;

    @JsonProperty
    private String description;

    @JsonProperty
    private Date createDate;

    @JsonProperty
    private Date updateDate;

    @JsonProperty
    private List<User> chairpersons;

    @JsonProperty
    private List<User> members;

    private List<DatasetDTO> datasets;

    private List<Integer> electionIds = new ArrayList<>();

    private List<Integer> datasetIds = new ArrayList<>();

    public Dac() {
    }

    public Integer getDacId() {
        return dacId;
    }

    public void setDacId(Integer dacId) {
        this.dacId = dacId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public List<User> getChairpersons() {
        return chairpersons;
    }

    public void setChairpersons(List<User> chairpersons) {
        this.chairpersons = chairpersons;
    }

    public List<User> getMembers() {
        return members;
    }

    public void setMembers(List<User> members) {
        this.members = members;
    }

    public List<Integer> getElectionIds() {
        return electionIds;
    }

    public void addElectionId(Integer electionId) {
        this.electionIds.add(electionId);
    }

    public List<Integer> getDatasetIds() {
        return datasetIds;
    }

    public void addDatasetId(Integer datasetId) {
        this.datasetIds.add(datasetId);
    }

    public void addDatasetDTO(DatasetDTO dto) {
        if ( Objects.isNull(datasets)) {
            datasets = new ArrayList<>();
        }
        datasets.add(dto);
    }
}
