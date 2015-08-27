package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.models.DataSet;

import java.io.File;
import java.io.InputStream;

public interface DataSetAPI {

    DataSet create(File dataSetFile);

    DataSet retrieve(String id) throws UnknownIdentifierException;
}
