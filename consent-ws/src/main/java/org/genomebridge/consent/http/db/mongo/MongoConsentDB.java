package org.genomebridge.consent.http.db.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;

public class MongoConsentDB {
        
    private final MongoClient mongo;
    public static final String DAR_CODE_COUNTER = "dar_code_counter";
    public static final String DAR_CODE = "dar_code";
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
     * @return researchPurposeCollection
     */
    public MongoCollection<Document> getResearchPurposeCollection() {
        return mongo.getDatabase("consent").getCollection("researchPurpose");
    }


    /**
     *
     * @return counters collection
     */
    public MongoCollection<Document> getCountersCollection() {
        return mongo.getDatabase("consent").getCollection("counters");
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

    public void configureMongo(){

    // Creates  MongoDB counter
    BasicDBObject searchCounter = new BasicDBObject().append("_id", DAR_CODE_COUNTER );
    if (getCountersCollection().find(searchCounter).first() == null) {
        Document counter = new Document("seq",0);
        counter.append("_id",DAR_CODE_COUNTER);
        getCountersCollection().insertOne(counter);

    }

    // Creates  MongoDB DataAccessRequest Index
    BasicDBObject indexFields = new BasicDBObject(DAR_CODE, 1);
    IndexOptions indexOptions = new IndexOptions();
    indexOptions.unique(true);
    indexOptions.sparse(true);
    getDataAccessRequestCollection().createIndex(indexFields, indexOptions);

    FindIterable<Document> documents = getDataAccessRequestCollection().find();
    documents.forEach((Block<Document>) dar -> {
        if (dar.get(DAR_CODE) == null) {
            BasicDBObject object = new BasicDBObject(DAR_CODE, "DAR-"+getNextSequence(DAR_CODE_COUNTER));
            BasicDBObject set = new BasicDBObject("$set", object);
            BasicDBObject searchQuery = new BasicDBObject().append("_id", dar.get("_id"));
            getDataAccessRequestCollection().updateOne(searchQuery, set);
        }
    });
    }
}
