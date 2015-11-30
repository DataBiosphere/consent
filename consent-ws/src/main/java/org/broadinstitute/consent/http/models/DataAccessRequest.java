/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.bson.Document;

/**
 *
 * @author dgil
 */
public class DataAccessRequest extends Document {
    private static final long serialVersionUID = 540392772361155266L;

    public DataAccessRequest() {
    }

    public Integer getId() {
        return this.getInteger("id");
    }

    @JsonProperty
    public void setId(Integer Id) {
        this.put("id", Id);
    }

    public String getRup() {
        return this.getString("rup");
    }

    @JsonProperty
    public void setRup(String rup) {
        this.put("rup", rup);
    }

    public String getCreateDate() {
        return this.getString("createDate");
    }

    @JsonProperty
    public void setCreateDate(String createDate) {
        this.put("createDate", createDate);
    }

}
