package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.models.DataSet;

import java.io.File;
import java.util.Collection;
import java.util.Map;

public interface DataSetAPI {

    Map<String, Object> create(File dataSetFile);

    Map<String, Object> overwrite(File dataSetFile);

    Collection<DataSet> describeDataSets() ;
}
