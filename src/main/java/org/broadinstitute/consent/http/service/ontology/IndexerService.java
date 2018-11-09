package org.broadinstitute.consent.http.service.ontology;

import com.google.inject.ImplementedBy;
import org.broadinstitute.consent.http.models.ontology.StreamRec;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@ImplementedBy(IndexerServiceImpl.class)
public interface IndexerService {
    
    Response saveAndIndex(List<StreamRec> streamRecList) throws  IOException;

    List<HashMap> getIndexedFiles() throws IOException;

    Response deleteOntologiesByType(String fileURL) throws IOException;

}
