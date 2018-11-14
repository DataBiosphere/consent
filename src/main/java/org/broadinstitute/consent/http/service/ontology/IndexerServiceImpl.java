package org.broadinstitute.consent.http.service.ontology;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.api.client.http.HttpResponse;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.models.ontology.StreamRec;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Created by SantiagoSaucedo on 3/11/2016.
 */
public class IndexerServiceImpl implements IndexerService {

    private final StoreOntologyService storeService;
    private final IndexOntologyService indexService;
    private static final ObjectMapper mapper = new ObjectMapper();


    public IndexerServiceImpl(StoreOntologyService storeService, IndexOntologyService indexService) {
        this.storeService = storeService;
        this.indexService = indexService;
    }


    @Override
    public Response saveAndIndex(List<StreamRec> streamRecList) throws IOException {
        indexService.indexOntologies(streamRecList);
        if (streamRecList.stream().anyMatch(s -> s.getAtLeastOneOntologyIndexed().equals(true))) {
            streamRecList = storeService.storeOntologies(streamRecList);
            String configString = storeService.retrieveConfigurationFile();
            if (StringUtils.isEmpty(configString)) {
                InputStream indexedUrlFiles = indexedOntologiesStreamBuilder(streamRecList);
                storeService.storeOntologyConfigurationFile(indexedUrlFiles);
            } else {
                Map<String, HashMap> configMap = parseAsMap(configString);
                streamRecList.stream().filter(StreamRec::getAtLeastOneOntologyIndexed).forEach(sr ->
                    addFileData(configMap, sr)
                );
                storeService.storeOntologyConfigurationFile(mapToStreamParser(configMap));
            }
            return okResponseBuilder(streamRecList);
        } else {
            return Response.notModified().build();
        }

    }

    private Response okResponseBuilder(List<StreamRec> streamRecList) {
        Map<String,HashMap> entityMap = new HashMap<>();
        for(StreamRec s : streamRecList){
            HashMap<String,Boolean> m = new HashMap<>();
            m.put("saveAndIndexed",s.getAtLeastOneOntologyIndexed());
            entityMap.put(s.getFileName(),m);
        }
        return  Response.ok().entity(entityMap).build();
    }



    @Override
    public Response deleteOntologiesByType(String fileURL) throws IOException {
        String configurationFileString = storeService.retrieveConfigurationFile();
        if (StringUtils.isEmpty(configurationFileString)) return Response.status(Response.Status.BAD_REQUEST).build();
        Map<String, HashMap> map = parseAsMap(configurationFileString);
        HttpResponse r = storeService.retrieveFile(fileURL);

        // Deprecate ontology terms
        Boolean deprecated = indexService.deprecateOntology((String) map.get(fileURL).get("ontologyType"));
        if (deprecated) {
            //Update configuration file
            deleteFileFromMap(map, fileURL);
            storeService.storeOntologyConfigurationFile(mapToStreamParser(map));

            //Delete file from CloudStorage
            storeService.deleteFile(fileURL);
            return Response.ok().build();
        }
        return Response.notModified().build();
    }

    private void deleteFileFromMap(Map<String, HashMap> configMap,String fileUrl) {
        configMap.remove(fileUrl);
    }

    @Override
    public List<HashMap> getIndexedFiles() throws IOException {
        List<HashMap> indexedFilesList = new ArrayList<>();
        String configurationFileString = storeService.retrieveConfigurationFile();
        if(StringUtils.isEmpty(configurationFileString)) return null;
        Map<String, HashMap> map = parseAsMap(configurationFileString);
        for(Map.Entry<String, HashMap> e : map.entrySet()){
            HashMap m = new HashMap<String,String>();
            m.put("fileUrl",e.getKey());
            for(Object k : e.getValue().keySet()){
                m.put(k.toString(),e.getValue().get(k));
            }
            indexedFilesList.add(m);
        }
        return indexedFilesList;
    }

     private Map<String, HashMap> parseAsMap(String str) throws IOException {
            ObjectReader reader = mapper.readerFor(Map.class);
            return reader.readValue(str);
    }

    private ByteArrayInputStream indexedOntologiesStreamBuilder(List<StreamRec> streamRecList){
        try {
            Map<String, HashMap> json = new HashMap<>();
            for (StreamRec streamRec : streamRecList) {
                if (streamRec.getAtLeastOneOntologyIndexed()) {
                    addFileData(json, streamRec);
                }
            }
            if (Objects.nonNull(json)) {
                return mapToStreamParser(json);
            }
            return null;
        }catch (JsonProcessingException e){
            throw new InternalServerErrorException();
        }
    }

    private void addFileData(Map<String, HashMap> json, StreamRec streamRec) {
        HashMap streamRecMap = new HashMap<String, String>();
        streamRecMap.put("fileName", streamRec.getFileName());
        streamRecMap.put("prefix", streamRec.getPrefix());
        streamRecMap.put("ontologyType", streamRec.getOntologyType());
        json.put(streamRec.getUrl(), streamRecMap);
    }

    private ByteArrayInputStream mapToStreamParser(Map<String, HashMap> json) throws JsonProcessingException {
        String content = new ObjectMapper().writeValueAsString(json);
        return new ByteArrayInputStream(content.getBytes());
    }
}

