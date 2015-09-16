package org.genomebridge.consent.http.service;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import javax.ws.rs.NotFoundException;
import java.util.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.genomebridge.consent.http.models.grammar.UseRestriction;


/**
 * Implementation class for MongoDataAccessRequestAPI.
 */
public class MongoDataAccessRequestAPI extends AbstractDataAccessRequestAPI {

    private MongoClient mongo;

    private UseRestrictionConverter converter;

    /**
     * Initialize the singleton API instance using the provided DAO. This method
     * should only be called once during application initialization (from the
     * run() method). If called a second time it will throw an
     * IllegalStateException. Note that this method is not synchronized, as it
     * is not intended to be called more than once.
     *
     * @param mongo The Data Access Object instance that the API should use to
     *            read/write data.
     */
    public static void initInstance(MongoClient mongo, UseRestrictionConverter converter) {
        DataAccessRequestAPIHolder.setInstance(new MongoDataAccessRequestAPI(mongo, converter));
    }

    /**
     * The constructor is private to force use of the factory methods and
     * enforce the singleton pattern.
     *
     * @param mongo The Data Access Object used to read/write data.
     */
    private MongoDataAccessRequestAPI(MongoClient mongo, UseRestrictionConverter converter) {
        this.mongo = mongo;
        this.converter = converter;
    }

    @Override
    public Document createDataAccessRequest(Document dataAccessRequest) throws IllegalArgumentException {
        mongo.getDatabase("consent").getCollection("dataAccessRequest").insertOne(dataAccessRequest);
        return dataAccessRequest;
    }

    @Override
    public Document describeDataAccessRequestById(String id) throws NotFoundException {
        BasicDBObject query = new BasicDBObject("_id", new ObjectId(id));
        return mongo.getDatabase("consent").getCollection("dataAccessRequest").find(query).first();
     }


    @Override
    public List<Document> describeDataAccessRequests() {
        List<Document> response = mongo.getDatabase("consent").getCollection("dataAccessRequest").find().into(new ArrayList<Document>());
        return response;
    }

    @Override
    public List<String> findDataSets(String partial) {
        Document query = new Document("objectId", new Document("$regex", partial));
        MongoDatabase db = mongo.getDatabase("consent");
        MongoCollection coll = db.getCollection("datasets");
        DistinctIterable response = coll.distinct("objectId", String.class).filter(query);
        Collection cor = response.into(new ArrayList<>());
        List<String> ids = new ArrayList<>();
        ids.addAll(cor);
        return ids;
    }

    @Override
    public UseRestriction createStructuredResearchPurpose(Document document){
        return  converter.parseJsonFormulary(document.toJson());
    }

    @Override
    public void deleteDataAccessRequest(Document document){
        BasicDBObject query = new BasicDBObject("_id", document.get("_id"));
        mongo.getDatabase("consent").getCollection("dataAccessRequest").findOneAndDelete(query);
    }

}



