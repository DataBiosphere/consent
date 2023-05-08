package org.broadinstitute.consent.http.models;

public class Dictionary {

    private Integer keyId;
    private String key;
    @Deprecated
    private Boolean required;
    @Deprecated
    private Integer displayOrder;
    @Deprecated
    private Integer receiveOrder;

    public Dictionary(Integer keyId, String key, Boolean required, Integer displayOrder, Integer receiveOrder) {
        this(key, required, displayOrder, receiveOrder);
        this.keyId = keyId;
    }

    public Dictionary(String key, Boolean required, Integer displayOrder, Integer receiveOrder) {
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

    @Deprecated
    public Boolean getRequired() {
        return required;
    }

    @Deprecated
    public void setRequired(Boolean required) {
        this.required = required;
    }

    @Deprecated
    public Integer getDisplayOrder() {
        return displayOrder;
    }

    @Deprecated
    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    @Deprecated
    public Integer getReceiveOrder() {
        return receiveOrder;
    }

    @Deprecated
    public void setReceiveOrder(Integer receiveOrder) {
        this.receiveOrder = receiveOrder;
    }
}