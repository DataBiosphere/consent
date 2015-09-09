package org.genomebridge.consent.http.db.mongo;

import javax.validation.constraints.NotNull;

public class MongoConfiguration {

    public String username;

    public String password;

    @NotNull
    public String uri;

    @NotNull
    public String dbName;
            
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

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }


}
