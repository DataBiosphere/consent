package org.genomebridge.consent.http.cloudstore;

import javax.validation.constraints.NotNull;

public class StoreConfiguration {

    public String username;

    public String password;

    @NotNull
    public String endpoint;

    @NotNull
    public String bucket;

    @NotNull
    public String type; // currently either S3 or GCS

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
