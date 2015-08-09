package org.genomebridge.consent.autocomplete;

import io.dropwizard.testing.junit.ResourceTestRule;
import io.dropwizard.testing.junit.ResourceTestRule.Builder;
import org.genomebridge.consent.autocomplete.resources.TranslateResource;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

public class TranslateTest {

    @ClassRule
    public static final ResourceTestRule gRule =
            new Builder().addResource(new TranslateResource()).build();

    @Test
    public void testPediatricCancerSample() {
        translate("sampleset",
                "{\"type\":\"and\",\"operands\":[{\"type\":\"named\",\"name\":\"http://purl.obolibrary.org/obo/DOID_162\"}," +
                "{\"type\":\"named\",\"name\":\"http://www.genomebridge.org/ontologies/DURPO/children\"}," +
                "{\"type\":\"named\",\"name\":\"http://www.genomebridge.org/ontologies/DURPO/Non_profit\"}]}",
                "Samples may only be used for the purpose of studying cancer. " +
                "In addition, samples may only be used for the study of children and may not be used for commercial purposes.");
    }

    @Test
    public void testBroadPurpose() {
        translate("purpose",
                "{\"type\":\"and\",\"operands\":[{\"type\":\"named\",\"name\":\"http://www.genomebridge.org/ontologies/DURPO/Broad\"}," +
                "{\"type\":\"named\",\"name\":\"http://www.genomebridge.org/ontologies/DURPO/Non_profit\"}]}",
                "Any sample which can be used for research at institutions in The Broad Institute.");
    }

    private void translate(String qType, String json, String expect) {
        Response response =
                gRule.client().target("/translate")
                        .queryParam("for", qType)
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .accept(MediaType.TEXT_PLAIN_TYPE)
                        .post(Entity.json(json));
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.readEntity(String.class)).isEqualTo(expect);
    }
}
