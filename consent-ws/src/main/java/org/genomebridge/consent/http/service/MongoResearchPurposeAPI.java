package org.genomebridge.consent.http.service;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.genomebridge.consent.http.models.ResearchPurpose;
import org.genomebridge.consent.http.models.grammar.UseRestriction;

import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Implementation class for ResearchPurposeAPI
 */
public class MongoResearchPurposeAPI extends AbstractResearchPurposeAPI {

    private MongoClient mongo;

    private Logger logger = Logger.getLogger("MongoResearchPurposeAPI");

    public static void initInstance(MongoClient mongo) {
        ResearchPurposeAPIHolder.setInstance(new MongoResearchPurposeAPI(mongo));

    }

    private MongoResearchPurposeAPI(MongoClient mongo) {
        this.mongo = mongo;
    }

    @Override
    public Document createResearchPurpose(ResearchPurpose researchPurpose) throws IllegalArgumentException {
        String restriction = new Gson().toJson(researchPurpose.get("purpose", Map.class));
        try {
            UseRestriction.parse(restriction);
            Document rp = new Document();
            getResearchPurposeCollection().insertOne(rp.append("purpose", restriction));
            return rp;
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

    }

    @Override
    public Document updateResearchPurpose(ResearchPurpose rec, String id) throws IllegalArgumentException, NotFoundException {
        String restriction = new Gson().toJson(rec.get("purpose", Map.class));
        Document rp = new Document();
        rp.append("purpose", restriction);
        BasicDBObject query = new BasicDBObject("_id", getObjectId(id));
        if (getResearchPurposeCollection().findOneAndReplace(query, rp) == null) {
            notFoundRP();
        }
        return describeResearchPurpose(id);
    }


    @Override
    public Document describeResearchPurpose(String id) throws NotFoundException {
        BasicDBObject query = new BasicDBObject("_id", getObjectId(id));
        Document document = getResearchPurposeCollection().find(query).first();
        if (document == null) notFoundRP();
        return document;
    }


    @Override
    public List<Document> describeResearchPurposes(String[] ids) throws NotFoundException {
        BasicDBObject orQuery = new BasicDBObject();
        List<BasicDBObject> query = new ArrayList<>();
        for (String id : ids) {
            try {
                query.add(new BasicDBObject("_id", new ObjectId(id)));
            } catch (Exception e) {
                logger.error("invalid id: " + id);
            }

        }
        orQuery.put("$or", query);
        return getResearchPurposeCollection().find(orQuery).into(new ArrayList<>());

    }

    @Override
    public void deleteResearchPurpose(String id) throws IllegalArgumentException, NotFoundException {
        BasicDBObject query = new BasicDBObject("_id", getObjectId(id));
        if (getResearchPurposeCollection().findOneAndDelete(query) == null) {
            notFoundRP();
        }
    }

    private Object getObjectId(String id) {
        try{
            return new ObjectId(id);
        }catch (IllegalArgumentException e){
            throw new NotFoundException("ResearchPurpose for the specified id does not exist");
        }

    }

    private MongoCollection<Document> getResearchPurposeCollection() {
        return mongo.getDatabase("consent").getCollection("researchPurpose");
    }

    private void notFoundRP() throws NotFoundException {
        throw new NotFoundException("ResearchPurpose for the specified id does not exist");
    }

}
