package org.genomebridge.consent.http.models.grammar;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.bson.Document;

import java.io.IOException;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Everything.class, name = "everything"),
        @JsonSubTypes.Type(value = Nothing.class, name = "nothing"),
        @JsonSubTypes.Type(value = Named.class, name = "named"),
        @JsonSubTypes.Type(value = Some.class, name = "some"),
        @JsonSubTypes.Type(value = Only.class, name = "only"),
        @JsonSubTypes.Type(value = And.class, name = "and"),
        @JsonSubTypes.Type(value = Or.class, name = "or"),
        @JsonSubTypes.Type(value = Not.class, name = "not")
})
public abstract class PurposeUseRestriction extends Document{


}
