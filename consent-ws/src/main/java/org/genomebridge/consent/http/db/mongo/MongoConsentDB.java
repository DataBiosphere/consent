package org.genomebridge.consent.http.db.mongo;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

public class MongoConsentDB {
        
    private final MongoClient mongo;

    /**
     *
     * @param mongo
     */
    public MongoConsentDB(MongoClient mongo) {
        this.mongo = mongo;
    }

    /**
     *
     * @return mongoClient
     */
    public MongoClient getMongoClient() {
        return mongo;
    }
    
    /**
     *
     * @return dataAccessRequest collection
     */
    public MongoCollection<Document> getDataAccessRequestCollection() {
        return mongo.getDatabase("consent").getCollection("dataAccessRequest");
    }
    
    /**
     *
     * @return reserachPurposeCollection
     */
    public MongoCollection<Document> getResearchPurposeCollection() {
        return mongo.getDatabase("consent").getCollection("researchPurpose");
    }
    
}
