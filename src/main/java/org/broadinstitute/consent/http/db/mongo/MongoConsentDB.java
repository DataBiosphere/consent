package org.broadinstitute.consent.http.db.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;

public class MongoConsentDB {

    private final MongoClient mongo;
    public static final String DAR_CODE = "dar_code";
    public static final String PARTIAL_DAR_CODE = "partial_dar_code";
    public String DATABASE_NAME;

    /**
     *
     * @param mongo
     */
    public MongoConsentDB(MongoClient mongo, String databaseName) {
        this.mongo = mongo;
        this.DATABASE_NAME = databaseName;
    }

    /**
     *
     * @return mongoClient
     */
    public MongoClient getMongoClient() {
        return mongo;
    }

    /**
     * @deprecated Replaced with DataAccessRequestService.getAllDataAccessRequestsAsDocuments()
     * @return dataAccessRequest collection
     */
    @Deprecated
    public MongoCollection<Document> getDataAccessRequestCollection() {
        return mongo.getDatabase(DATABASE_NAME).getCollection("dataAccessRequest");
    }

    /**
     *
     * @return dataAccessRequest collection
     */
    public MongoCollection<Document> getPartialDataAccessRequestCollection() {
        return mongo.getDatabase(DATABASE_NAME).getCollection("dataAccessRequestPartials");
    }

    public void configureMongo() {

        IndexOptions indexOptions = new IndexOptions();
        indexOptions.unique(true);
        indexOptions.sparse(true);

        // Creates MongoDB Partial DataAccessRequest Index
        BasicDBObject secondIndexFields = new BasicDBObject(PARTIAL_DAR_CODE, 1);
        getPartialDataAccessRequestCollection().createIndex(secondIndexFields, indexOptions);

    }
}
