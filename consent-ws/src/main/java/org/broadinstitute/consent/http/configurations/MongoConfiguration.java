package org.broadinstitute.consent.http.configurations;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class MongoConfiguration {

    public String username;

    public String password;

    @NotNull
    public String uri;

    public String host1;

    public String host2;

    @NotNull
    public String dbName;
    
    public boolean testMode = false;
            
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    @SuppressWarnings("unused")
    public void setPassword(String password) {
        this.password = password;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getHost1() {
        return host1;
    }

    @SuppressWarnings("unused")
    public void setHost1(String host1) {
        this.host1 = host1;
    }

    public String getHost2() {
        return host2;
    }

    @SuppressWarnings("unused")
    public void setHost2(String host2) {
        this.host2 = host2;
    }

    public String getDbName() {
        return dbName;
    }

    @SuppressWarnings("unused")
    public void setDbName(String dbName) {
        this.dbName = dbName;
    }
    
    public Boolean isTestMode() {
        return testMode;
    }

    @SuppressWarnings("unused")
    public Boolean getTestMode() {
        return testMode;
    }

    @SuppressWarnings("unused")
    public void setTestMode(Boolean testMode) {
        this.testMode = testMode;
    }

    public MongoClient getMongoClient() {
        if ( StringUtils.isBlank(getHost1())  || StringUtils.isBlank(getHost2()) ) {
            return new MongoClient(new MongoClientURI(getUri()));
        } else if (getHost1() != null && getHost2() != null) {
            List<ServerAddress> seeds = new ArrayList<>();
            seeds.add(new ServerAddress(getHost1()));
            seeds.add(new ServerAddress(getHost2()));
            MongoCredential credential = MongoCredential.createCredential(getUsername(), getDbName(), getPassword().toCharArray());
            return new MongoClient(seeds, Collections.singletonList(credential));
        }
        return null;
    }


}
