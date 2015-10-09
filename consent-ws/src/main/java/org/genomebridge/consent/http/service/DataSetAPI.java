package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.models.DataSet;
import org.genomebridge.consent.http.models.Dictionary;
import org.genomebridge.consent.http.models.dto.DataSetDTO;

import java.io.File;
import java.util.Collection;
import java.util.List;

public interface DataSetAPI {

    ParseResult create(File dataSetFile);

    ParseResult overwrite(File dataSetFile);

    Collection<DataSetDTO> describeDataSets() ;

    List<DataSet> getDataSetsForConsent(String consentId);

    Collection<DataSetDTO> describeDataSets(List<String> objectIds) ;

    Collection<Dictionary> describeDictionary();
    
    List<String> autoCompleteDataSets(String partial);
            
}
