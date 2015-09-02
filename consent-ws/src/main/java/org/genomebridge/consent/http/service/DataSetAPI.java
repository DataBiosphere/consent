package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.models.Dictionary;
import org.genomebridge.consent.http.models.dto.DataSetDTO;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface DataSetAPI {

    Map<String, Object> create(File dataSetFile);

    Map<String, Object> overwrite(File dataSetFile);

    Collection<DataSetDTO> describeDataSets() ;

    Collection<DataSetDTO> describeDataSets(List<String> objectIds) ;

    Collection<Dictionary> describeDictionary();
    }
