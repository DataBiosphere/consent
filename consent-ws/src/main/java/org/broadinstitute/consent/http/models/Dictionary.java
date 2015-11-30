package org.broadinstitute.consent.http.models;

import java.util.Date;

public class Dictionary {

    private Integer keyId;
    private String key;
    private Boolean required;
    private Integer displayOrder;
    private Date createDate;

    public Dictionary(Integer keyId, String key, Boolean required, Integer displayOrder){
        this(key, required, displayOrder);
        this.keyId = keyId;
    }

    public Dictionary(String key, Boolean required, Integer displayOrder){
        this.key = key;
        this.required = required;
        this.displayOrder = displayOrder;
    }

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

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
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