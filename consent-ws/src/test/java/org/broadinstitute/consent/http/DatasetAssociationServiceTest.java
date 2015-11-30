package org.broadinstitute.consent.http;

public abstract class DatasetAssociationServiceTest extends AbstractTest {

    public String datasetAssociationPath(String objectId) {
        return path2Url(String.format("/datasetAssociation/%s", objectId));

    }
}
