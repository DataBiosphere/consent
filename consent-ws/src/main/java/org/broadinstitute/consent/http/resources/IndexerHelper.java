package org.broadinstitute.consent.http.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.consent.http.enumeration.OntologyTypes;
import org.broadinstitute.consent.http.models.ontology.StreamRec;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IndexerHelper {

    public List<StreamRec> filesCompBuilder(FormDataMultiPart formParams) throws IOException {
        List<StreamRec> fileRecList = new ArrayList<>();
        Map<String,InputStream> formParts =  formParams.getFields().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0).getEntityAs(InputStream.class)));
        Map<String,FormDataBodyPart> contentDispositionList =  formParams.getFields().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));
        InputStream metadataStream =  formParts.get("metadata");
        if(metadataStream == null){
            throw new IOException("Expected parameter 'metadata' wasn't sent.");
        }
        formParts.remove("metadata");
        StringWriter writer = new StringWriter();
        IOUtils.copy(metadataStream, writer);
        Map<String, LinkedHashMap> metadataMap = parseAsMap( writer.toString());
        for(Map.Entry<String, InputStream> entry: formParts.entrySet()){
            if(formParts.get(entry.getKey()) == null){
                throw new IOException("Metadata key " + entry.getKey() + " doesn't have corresponding value.");
            }
            LinkedHashMap<String,String> m = metadataMap.get(entry.getKey());
            FormDataBodyPart bp = contentDispositionList.get(entry.getKey());
            if(!m.containsKey("type") || !m.containsKey("prefix")){
                throw new IOException("Metadata doesn't contain expected element 'type' or 'prefix'");
            }
            if(!OntologyTypes.contains(m.get("type"))){
                throw new IOException(m.get("type")+" is an invalid OntologyType.");
            }
            fileRecList.add(new StreamRec(formParts.get(entry.getKey()), m.get("type"),m.get("prefix"),bp.getMediaType().toString(),bp.getContentDisposition().getFileName()));
        }
    return fileRecList;
    }

    private Map<String, LinkedHashMap> parseAsMap(String str) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectReader reader = mapper.readerFor(Map.class);
        return reader.readValue(str);
    }
}
