package org.genomebridge.consent.http.service;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.util.JSON;
import org.apache.commons.collections.CollectionUtils;
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
    public ResearchPurpose createResearchPurpose(ResearchPurpose researchPurpose) throws IllegalArgumentException {
        String restriction = new Gson().toJson(researchPurpose.get("restriction", Map.class));
        DBObject dbObject = (DBObject) JSON.parse(restriction);
        try {
            UseRestriction.parse(restriction);
            Document rp = new Document();
            getResearchPurposeCollection().insertOne(rp.append("restriction", dbObject));
            return getResearchPurpose(rp);
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

    }

    @Override
    public ResearchPurpose updateResearchPurpose(ResearchPurpose rec, String id) throws IllegalArgumentException, NotFoundException, IOException {
        String restriction = new Gson().toJson(rec.get("restriction", Map.class));
        DBObject dbObject = (DBObject) JSON.parse(restriction);
        Document rp = new Document();
        rp.append("restriction", dbObject);
        BasicDBObject query = new BasicDBObject("_id", getObjectId(id));
        if (getResearchPurposeCollection().findOneAndReplace(query, rp) == null) {
            notFoundRP();
        }
        return describeResearchPurpose(id);
    }


    @Override
    public ResearchPurpose describeResearchPurpose(String id) throws NotFoundException, IOException {
        BasicDBObject query = new BasicDBObject("_id", getObjectId(id));
        Document document = getResearchPurposeCollection().find(query).first();
        if (document == null) notFoundRP();
        return getResearchPurpose(document);
    }


    @Override
    public List<ResearchPurpose> describeResearchPurposes(String[] ids) throws NotFoundException, IOException {
        List<ResearchPurpose> rpResult = new ArrayList<>();
        BasicDBObject orQuery = new BasicDBObject();
        List<BasicDBObject> query = new ArrayList<>();
        setQueryFilter(ids, query);
        orQuery.put("$or", query);
        List<Document> documents = getResearchPurposeCollection().find(orQuery).into(new ArrayList<>());
        if(CollectionUtils.isNotEmpty(documents)){
            for(Document rp : documents){
                rpResult.add(getResearchPurpose(rp));
            }
        }
        return rpResult;
    }

    private void setQueryFilter(String[] ids, List<BasicDBObject> query) {
        for (String id : ids) {
            try {
                query.add(new BasicDBObject("_id", new ObjectId(id)));
            } catch (Exception e) {
                logger.error("invalid id: " + id);
            }

        }
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

    private ResearchPurpose getResearchPurpose(Document rp) throws IOException{
       String rest = new Gson().toJson(rp.get("restriction", Map.class));
       return new ResearchPurpose(rp.get("_id", ObjectId.class).toString(), UseRestriction.parse(rest));
    }

}
