package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;


public class ApprovalExpirationTime {

    @JsonProperty
    private Integer id;

    @JsonProperty
    private Integer userId;

    @JsonProperty
    private Date createDate;

    @JsonProperty
    private Date updateDate;

    @JsonProperty
    private Integer amountOfDays;

    @JsonProperty
    private String displayName;


    public ApprovalExpirationTime() {
    }

    public ApprovalExpirationTime(Integer id, Integer userId, Date createDate, Date updateDate, Integer amountOfDays, String displayName) {
        this.id = id;
        this.userId = userId;
        this.amountOfDays = amountOfDays;
        this.updateDate = updateDate;
        this.createDate = createDate;
        this.displayName = displayName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
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

    public Integer getAmountOfDays() {
        return amountOfDays;
    }

    public void setAmountOfDays(Integer amountOfDays) {
        this.amountOfDays = amountOfDays;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
