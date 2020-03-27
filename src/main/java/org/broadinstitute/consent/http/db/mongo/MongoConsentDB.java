package org.broadinstitute.consent.http.db.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;

public class MongoConsentDB {

    private final MongoClient mongo;
    public static final String DAR_CODE_COUNTER = "dar_code_counter";
    public static final String PARTIAL_DAR_CODE_COUNTER = "partial_dar_code_counter";
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
     *
     * @return dataAccessRequest collection
     */
    public MongoCollection<Document> getPartialDataAccessRequestCollection() {
        return mongo.getDatabase(DATABASE_NAME).getCollection("dataAccessRequestPartials");
    }

    /**
     *
     * @return counters collection
     */
    public MongoCollection<Document> getCountersCollection() {
        return mongo.getDatabase(DATABASE_NAME).getCollection("counters");
    }

    /**
     *
     * @return retrieves the current seq of a counter
     */
    public String getNextSequence(String name) {
        BasicDBObject query = new BasicDBObject();
        query.put("_id", name);
        BasicDBObject update = new BasicDBObject();
        update.append("$inc", new BasicDBObject().append("seq", 1));
        Document rec = getCountersCollection().findOneAndUpdate(query, update);
        return rec.get("seq").toString();
    }

    public void configureMongo() {

        // Creates  MongoDB counter
        BasicDBObject searchCounter = new BasicDBObject().append(DarConstants.ID, DAR_CODE_COUNTER);
        if (getCountersCollection().find(searchCounter).first() == null) {
            Document counter = new Document("seq", 0);
            counter.append(DarConstants.ID, DAR_CODE_COUNTER);
            getCountersCollection().insertOne(counter);
        }

        BasicDBObject partialCounter = new BasicDBObject().append(DarConstants.ID, PARTIAL_DAR_CODE_COUNTER);
        if (getCountersCollection().find(partialCounter).first() == null) {
            Document counter = new Document("seq", 0);
            counter.append(DarConstants.ID, PARTIAL_DAR_CODE_COUNTER);
            getCountersCollection().insertOne(counter);
        }

        IndexOptions indexOptions = new IndexOptions();
        indexOptions.unique(true);
        indexOptions.sparse(true);

        // Creates MongoDB Partial DataAccessRequest Index
        BasicDBObject secondIndexFields = new BasicDBObject(PARTIAL_DAR_CODE, 1);
        getPartialDataAccessRequestCollection().createIndex(secondIndexFields, indexOptions);

        FindIterable<Document> partialDocuments = getPartialDataAccessRequestCollection().find();
        partialDocuments.forEach((Block<Document>) dar -> {
            if (dar.get(PARTIAL_DAR_CODE) == null) {
                BasicDBObject object = new BasicDBObject(PARTIAL_DAR_CODE, "PARTIAL DAR-" + getNextSequence(PARTIAL_DAR_CODE_COUNTER));
                BasicDBObject set = new BasicDBObject("$set", object);
                BasicDBObject searchQuery = new BasicDBObject().append(DarConstants.ID, dar.get("_id"));
                getPartialDataAccessRequestCollection().updateOne(searchQuery, set);
            }
        });
    }
}
