package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import org.broadinstitute.consent.http.util.JsonSchemaUtil;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

public class SchemaResourceTest {


    private final JsonSchemaUtil jsonSchemaUtil = new JsonSchemaUtil();

    @Test
    public void testGetDatasetRegistrationSchemaV1() {
        SchemaResource resource = new SchemaResource();

        Response response = resource.getDatasetRegistrationSchemaV1();
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());

        Object body = response.getEntity();
        assertEquals(jsonSchemaUtil.getDatasetRegistrationSchemaV1(), body.toString());
    }

}
