package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.util.JsonSchemaUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SchemaResourceTest {


    private final JsonSchemaUtil jsonSchemaUtil = new JsonSchemaUtil();

    @Test
    public void testGetDatasetRegistrationSchemaV1() {
        SchemaResource resource = new SchemaResource();

        Response response = resource.getDatasetRegistrationSchemaV1();
        Assertions.assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());

        Object body = response.getEntity();
        Assertions.assertEquals(jsonSchemaUtil.getDatasetRegistrationSchemaV1(), body.toString());
    }

}
