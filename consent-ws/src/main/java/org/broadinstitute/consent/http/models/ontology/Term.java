package org.broadinstitute.consent.http.models.ontology;

import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class Term {
    private static String FIELD_ID = "id";
    public static String FIELD_ONTOLOGY_TYPE = "ontology";
    private static String FIELD_LABEL = "label";
    private static String FIELD_DEFINITION = "definition";
    private static String FIELD_SYNONYM = "synonyms";
    private static String FIELD_USABLE = "usable";
    private static String FIELD_PARENTS = "parents";

    private String id;
    private String ontologyType;
    private List<String> synonyms;
    private String label;
    private String definition;
    private Boolean usable;
    private Map<String, Object> parents;

    public Term(String id, String ontologyType) {
        this.id = id;
        this.ontologyType = ontologyType;
        this.synonyms = new ArrayList<>();
        this.usable = true;
        this.parents = new HashMap<>();
    }

    public void addSynonym(String synonym) {
        synonyms.add(synonym);
    }

    public void addLabel(String label) {
        this.label = label;
    }

    public void addDefinition(String definition) {
        this.definition = definition;
    }

    public void setUsable(boolean useable) {
        this.usable = useable;
    }

    public XContentBuilder document() throws IOException {
        XContentBuilder builder = jsonBuilder()
                .startObject()
                .field(FIELD_ID, id)
                .field(FIELD_ONTOLOGY_TYPE, ontologyType)
                .field(FIELD_USABLE, usable);
        if (label != null) {
            builder = builder.field(FIELD_LABEL, label);
        }
        if (definition != null) {
            builder = builder.field(FIELD_DEFINITION, definition);
        }
        if (synonyms.size() != 0) {
            builder = builder.array(FIELD_SYNONYM, synonyms.toArray(new String[synonyms.size()]));
        }

        if (!parents.isEmpty()) {
            builder.startArray(FIELD_PARENTS);
            for (Map.Entry<String, Object> entry : parents.entrySet()) {
                builder.startObject();
                builder.field(FIELD_ID, entry.getKey());
                builder.field("order", entry.getValue());
                builder.endObject();
            }
            builder.endArray();
        }

        return builder.endObject();
    }

    public String getId() { return id; }

    public void addParent(String parent, Integer position) {
        parents.put(parent, position);
    }

    @Override
    public String toString() {
        try {
            return document().prettyPrint().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return getId();
    }

}
