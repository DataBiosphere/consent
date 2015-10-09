package org.genomebridge.consent.http.service;

import com.mongodb.BasicDBObject;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.genomebridge.consent.http.db.mongo.MongoConsentDB;
import org.genomebridge.consent.http.models.grammar.UseRestriction;
import static com.mongodb.client.model.Filters.*;

import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.List;


/**
 * Implementation class for DatabaseDataAccessRequestAPI.
 */
public class DatabaseDataAccessRequestAPI extends AbstractDataAccessRequestAPI {

    private final MongoConsentDB mongo;

    private final UseRestrictionConverter converter;

    /**
     * Initialize the singleton API instance using the provided DAO. This method
     * should only be called once during application initialization (from the
     * run() method). If called a second time it will throw an
     * IllegalStateException. Note that this method is not synchronized, as it
     * is not intended to be called more than once.
     *
     * @param mongo     The Data Access Object instance that the API should use to
     *                  read/write data.
     * @param converter
     */
    public static void initInstance(MongoConsentDB mongo, UseRestrictionConverter converter) {
        DataAccessRequestAPIHolder.setInstance(new DatabaseDataAccessRequestAPI(mongo, converter));
    }

    /**
     * The constructor is private to force use of the factory methods and
     * enforce the singleton pattern.
     *
     * @param mongo The Data Access Object used to read/write data.
     */
    private DatabaseDataAccessRequestAPI(MongoConsentDB mongo, UseRestrictionConverter converter) {
        this.mongo = mongo;
        this.converter = converter;
     }

    @Override
    public Document createDataAccessRequest(Document dataAccessRequest) throws IllegalArgumentException {
        mongo.getDataAccessRequestCollection().insertOne(dataAccessRequest);
        return dataAccessRequest;
    }

    @Override
    public Document describeDataAccessRequestById(String id) throws NotFoundException {
        BasicDBObject query = new BasicDBObject("_id", new ObjectId(id));
        return mongo.getDataAccessRequestCollection().find(query).first();
    }

    // DO NOT USE this method to get the UseRestriction field ("restriction")
    // Use getUseRestriction instead.
    @Override
    public void deleteDataAccessRequestById(String id) {
        BasicDBObject query = new BasicDBObject("_id", new ObjectId(id));
        mongo.getDataAccessRequestCollection().findOneAndDelete(query);
    }

    @Override
    public Document describeDataAccessRequestFieldsById(String id, List<String> fields) throws NotFoundException {
        BasicDBObject query = new BasicDBObject("_id", new ObjectId(id));
        Document dar = mongo.getDataAccessRequestCollection().find(query).first();
        Document result = new Document();
        for (String field : fields) {
            String content = (String) dar.getOrDefault(field.replaceAll("\\s", ""), "Not found");
            result.append(field, content);
        }
        return result;
    }

    @Override
    public List<Document> describeDataAccessWithDataSetId(List<String> dataSetIds){
        List<Document> response = new ArrayList<>();
        for(String datasetId: dataSetIds){
            response.addAll(mongo.getDataAccessRequestCollection().find(eq("datasetId", datasetId)).into(new ArrayList<>()));
        }
        return response;
    }

    @Override
    public List<Document> describeDataAccessRequests() {
        List<Document> response = mongo.getDataAccessRequestCollection().find().into(new ArrayList<>());
        return response;
    }

    @Override
    public UseRestriction createStructuredResearchPurpose(Document document) {
        return converter.parseJsonFormulary(document.toJson());
    }

    @Override
    public void deleteDataAccessRequest(Document document) {
        BasicDBObject query = new BasicDBObject("_id", document.get("_id"));
        mongo.getDataAccessRequestCollection().findOneAndDelete(query);
    }

}



