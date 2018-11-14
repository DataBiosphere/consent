package org.broadinstitute.consent.http.models;

import java.util.Date;

public class Dictionary {

    private Integer keyId;
    private String key;
    private Boolean required;
    private Integer displayOrder;
    private Date createDate;
    private Integer receiveOrder;

    public Dictionary(Integer keyId, String key, Boolean required, Integer displayOrder, Integer receiveOrder){
        this(key, required, displayOrder, receiveOrder);
        this.keyId = keyId;
    }

    public Dictionary(String key, Boolean required, Integer displayOrder, Integer receiveOrder){
        this.key = key;
        this.required = required;
        this.displayOrder = displayOrder;
        this.receiveOrder = receiveOrder;
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

    public Integer getReceiveOrder() {
        return receiveOrder;
    }

    public void setReceiveOrder(Integer receiveOrder) {
        this.receiveOrder = receiveOrder;
    }
}