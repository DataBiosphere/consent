package org.genomebridge.consent.http.models;

import java.util.Date;

public class Dictionary {

    private Integer keyId;
    private String key;
    private Boolean requiered;
    private Integer displayOrder;
    private Date createDate;

    public Integer getKeyId() {
        return keyId;
    }

    public void setKeyId(Integer keyId) {
        this.keyId = keyId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Boolean getRequiered() {
        return requiered;
    }

    public void setRequiered(Boolean requiered) {
        this.requiered = requiered;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }
}