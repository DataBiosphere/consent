package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.List;

/**
 * Entity representing a Data Access Committee
 */
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
    private List<DACUser> chairpersons;

    @JsonProperty
    private List<DACUser> members;

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

    public List<DACUser> getChairpersons() {
        return chairpersons;
    }

    public void setChairpersons(List<DACUser> chairpersons) {
        this.chairpersons = chairpersons;
    }

    public List<DACUser> getMembers() {
        return members;
    }

    public void setMembers(List<DACUser> members) {
        this.members = members;
    }

}
