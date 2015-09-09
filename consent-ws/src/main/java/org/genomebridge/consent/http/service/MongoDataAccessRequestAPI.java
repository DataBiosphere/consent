package org.genomebridge.consent.http.service;

import com.mongodb.MongoClient;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import javax.ws.rs.NotFoundException;
import java.util.*;
import org.bson.Document;
import org.genomebridge.consent.http.models.DataAccessRequest;


/**
 * Implementation class for VoteAPI on top of ElectionDAO database support.
 */
public class MongoDataAccessRequestAPI extends AbstractDataAccessRequestAPI {

    private MongoClient mongo;

    /**
     * Initialize the singleton API instance using the provided DAO. This method
     * should only be called once during application initialization (from the
     * run() method). If called a second time it will throw an
     * IllegalStateException. Note that this method is not synchronized, as it
     * is not intended to be called more than once.
     *
     * @param userDao The Data Access Object instance that the API should use to
     *            read/write data.
     */
    public static void initInstance(MongoClient mongo) {
        DataAccessRequestAPIHolder.setInstance(new MongoDataAccessRequestAPI(mongo));
    }

    /**
     * The constructor is private to force use of the factory methods and
     * enforce the singleton pattern.
     *
     * @param userDAO The Data Access Object used to read/write data.
     */
    private MongoDataAccessRequestAPI(MongoClient mongo) {
        this.mongo = mongo;
    }

    @Override
    public Document createDataAccessRequest(Document dataAccessRequest) throws IllegalArgumentException {
//        Document dar = new Document("id", dataAccessRequest.getId());
//        dar.append("rup", dataAccessRequest.getRup());
//        dar.append("createDate", dataAccessRequest.getCreateDate());
        mongo.getDatabase("consent").getCollection("dataAccessRequest").insertOne(dataAccessRequest);
        return dataAccessRequest;
    }

    @Override
    public Document describeDataAccessRequestById(String id) throws NotFoundException {
        return new Document();
    }

    @Override
    public DataAccessRequest updateDataAccessRequest(DataAccessRequest rec, String Id) throws IllegalArgumentException, NotFoundException {
        return new DataAccessRequest();
    }

    @Override
    public void deleteDataAccessRequest(String id) throws IllegalArgumentException, NotFoundException {
        return;
    }

    @Override
    public List<Document> describeDataAccessRequests() {
        Collection<DataAccessRequest> dars = new ArrayList<>();
        List<Document> response = mongo.getDatabase("consent").getCollection("dataAccessRequest").find().into(new ArrayList<Document>());
        return response;
    }

    @Override
    public List<String> findDataSets(String partial) {
        Collection<DataAccessRequest> dars = new ArrayList<>();
        Document query = new Document("objectId", new Document("$regex", partial));
        
        Document fields = new Document("_id", 0).append("objectId", 1);
        MongoDatabase db = mongo.getDatabase("consent");
        MongoCollection coll = db.getCollection("datasets");
        DistinctIterable response = coll.distinct("objectId", String.class).filter(query);
        Collection cor = response.into(new ArrayList<>());
        List<String> ids = new ArrayList<>();
        ids.addAll(cor);
        return ids;
    }
  
}



