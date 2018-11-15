package org.broadinstitute.consent.http.models.grammar;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.gson.Gson;
import org.apache.log4j.Logger;

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
public abstract class UseRestriction {

    private static final Logger LOG = Logger.getLogger(UseRestriction.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static UseRestriction parse(String str) throws IOException {
        try {
            ObjectReader reader = mapper.readerFor(UseRestriction.class);
            return reader.readValue(str);

        } catch (IOException e) {
            LOG.error(String.format("Parse exception on \"%s\"", str));
            throw e;
        }
    }

    public boolean visit(UseRestrictionVisitor visitor) {
        boolean shouldContinue = true;
        if ((shouldContinue = visitor.visit(this))) {
            visitor.startChildren();
            shouldContinue = visitAndContinue(visitor);
            visitor.endChildren();
        }

        return shouldContinue;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public abstract boolean visitAndContinue(UseRestrictionVisitor visitor);
}
