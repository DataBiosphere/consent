package org.broadinstitute.consent.http.service.ontology;

import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import org.broadinstitute.consent.http.cloudstore.CloudStore;
import org.broadinstitute.consent.http.enumeration.OntologyTypes;
import org.broadinstitute.consent.http.models.ontology.StreamRec;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * Created by SantiagoSaucedo on 3/11/2016.
 */
public class StoreOntologyService   {

    private final CloudStore store;
    private final String bucketSubdirectory;
    private final String configurationFileName;
    private final String jsonExtension = ".json";

    public StoreOntologyService(CloudStore store, String bucketSubdirectory, String configurationFileName) {
        this.store = store;
        this.bucketSubdirectory = bucketSubdirectory;
        this.configurationFileName = configurationFileName;
    }


    public void storeOntologyConfigurationFile(InputStream inputStream)   {
        String type = MediaType.APPLICATION_JSON;
        try {
            String url_suffix =  bucketSubdirectory + configurationFileName + jsonExtension ;
            store.postStorageDocument(inputStream,
                    type,
                    url_suffix);
        }catch (IOException | GeneralSecurityException e) {
            throw new InternalServerErrorException("Problem with storage service. (Error 10)");
        }
    }

    public List<StreamRec> storeOntologies(List<StreamRec> streamRecList){
        for(StreamRec srec : streamRecList) {
            if (srec.getAtLeastOneOntologyIndexed()) {
                try {
                    String url_suffix = bucketSubdirectory + "/" + OntologyTypes.getValue(srec.getOntologyType()) + "/" + srec.getFileName();
                    srec.setUrl(store.postOntologyDocument(srec.getStream(),
                            srec.getFileType(),
                            url_suffix));
                }catch (IOException | URISyntaxException | GeneralSecurityException e) {
                    throw new InternalServerErrorException("Problem with storage service. (Error 20)");
                }
            }
        }
        return streamRecList;
    }

    public String retrieveConfigurationFile (){
        try {
            HttpResponse response = store.getStorageDocument(store.generateURLForDocument(bucketSubdirectory + configurationFileName + jsonExtension).toString());
            return response.parseAsString();
        } catch (Exception e) {
            if (e instanceof HttpResponseException && ((HttpResponseException) e).getStatusCode() == 404) {
                return null;
            } else {
                throw new InternalError("Problem with storage service. (Error 30)");
            }
        }
    }

    public HttpResponse retrieveFile(String fileUrl){
        try {
            return store.getStorageDocument(fileUrl);
        } catch (Exception e) {
            throw new InternalError("Problem with storage service. (Error 40)");
        }
    }

    public void  deleteFile(String fileUrl){
        try {
            store.deleteStorageDocument(fileUrl);
        }catch (Exception e) {
            throw new InternalError("Problem with storage service. (Error 50)");
        }
    }
}
