package org.broadinstitute.consent.http;

import com.codahale.metrics.health.HealthCheck;
import com.mongodb.MongoClient;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;

public class MongoHealthCheck extends HealthCheck {
 
    private final MongoClient mongo;
    private final String databaseName;
 
    protected MongoHealthCheck(MongoClient mongo, String databaseName) {
        super();
        this.mongo = mongo;
        this.databaseName = databaseName;
    }
 
    @Override
    protected Result check() throws Exception {

        // https://stackoverflow.com/a/16823314/818054
        BasicDBObject ping = new BasicDBObject("ping", "1");

        try {
            mongo.getDatabase(this.databaseName).runCommand(ping);
        } catch (MongoException e) {
            return Result.unhealthy("Exception querying Mongo instance: " + e.getMessage());
        }

        return Result.healthy();
    }
 
}
