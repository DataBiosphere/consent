package org.broadinstitute.consent.http;

public abstract class DatasetAssociationServiceTest extends AbstractTest {

    public String datasetAssociationPath(Integer datasetId) {
        return path2Url(String.format("/datasetAssociation/%s", datasetId));

    }
}
