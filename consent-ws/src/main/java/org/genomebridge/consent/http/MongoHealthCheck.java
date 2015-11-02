package org.genomebridge.consent.http;

import com.codahale.metrics.health.HealthCheck;
import com.mongodb.Mongo;

public class MongoHealthCheck extends HealthCheck {
 
    private final Mongo mongo;
 
    protected MongoHealthCheck(Mongo mongo) {
        super();
        this.mongo = mongo;
    }
 
    @Override
    protected Result check() throws Exception {
        mongo.getDatabaseNames();
        return Result.healthy();
    }
 
}
