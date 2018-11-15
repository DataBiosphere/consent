package org.broadinstitute.consent.http.cloudstore;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.ObjectAccessControl;
import com.google.api.services.storage.model.StorageObject;
import org.apache.log4j.Logger;
import org.broadinstitute.consent.http.configurations.StoreConfiguration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;

public class GCSStore implements CloudStore {

    private final static HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    private final StoreConfiguration sConfig;
    HttpRequestFactory requestFactory;
    GoogleCredential credential;

    protected Logger logger() {
        return Logger.getLogger("GCSStore");
    }

    private void initializeCloudStore(){
        if(credential == null){
            credential = authorize();
            requestFactory = HTTP_TRANSPORT.createRequestFactory(credential);
        }
    }

    public GCSStore(StoreConfiguration config) throws GeneralSecurityException, IOException {
        sConfig = config;
        credential = null;
        requestFactory = null;
    }

    @Override
    public GenericUrl generateURLForDocument(String fileName) {
        return new GenericUrl(sConfig.getEndpoint() + sConfig.getBucket() + "/" + fileName);
    }

    /**
     * Authorizes the installed application to access user's protected data.
     */
    private GoogleCredential authorize() {
        GoogleCredential credential;
        try {
            credential = GoogleCredential.
                    fromStream(new FileInputStream(sConfig.getPassword())).
                    createScoped(Collections.singletonList(StorageScopes.DEVSTORAGE_FULL_CONTROL));
        } catch (Exception e) {
            logger().error("Error on GCS Store initialization. Service won't work: " + e);
            throw new RuntimeException(e);
        }
        return credential;
    }

    private HttpRequest buildHttpDeleteRequest(GenericUrl url) throws IOException, GeneralSecurityException {
        HttpRequest request = requestFactory.buildDeleteRequest(url);
        return request;
    }

    @Override
    public boolean deleteStorageDocument(String documentUrl) throws IOException, GeneralSecurityException {
        HttpResponse response = null;
        try {
            initializeCloudStore();
            HttpRequest request = buildHttpDeleteRequest(new GenericUrl(documentUrl));
            response = request.execute();
            return true;
        } finally {
            if (response != null) {
                try {
                    response.disconnect();
                } catch (IOException e) {
                    logger().error("Error disconnecting response.", e);
                }
            }
        }
    }

    private HttpRequest buildHttpGetRequest(GenericUrl url) throws IOException, GeneralSecurityException {
        return requestFactory.buildGetRequest(url);
    }

    @Override
    public HttpResponse getStorageDocument(String documentUrl) throws IOException, GeneralSecurityException {
        HttpResponse response;
        initializeCloudStore();
        HttpRequest request = buildHttpGetRequest(new GenericUrl(documentUrl));
        response = request.execute();
        return response;
    }

    private HttpRequest buildHttpPutRequest(GenericUrl url, HttpContent content) throws IOException, GeneralSecurityException {
        HttpRequest request = requestFactory.buildPutRequest(url, content);
        return request;
    }

    @Override
    public String postStorageDocument(InputStream stream, String type, String fileName) throws IOException, GeneralSecurityException {
        GenericUrl url = generateURLForDocument(fileName);
        HttpResponse response = null;
        try {
            initializeCloudStore();
            HttpContent content = new InputStreamContent(type, stream);
            HttpRequest request = buildHttpPutRequest(url, content);
            request.getHeaders().setCacheControl("private");
            response = request.execute();
            if (response.getStatusCode() != 200) {
                logger().error("Error storing contents: " + response.getStatusMessage());
            }
        } finally {
            if (null != response) {
                try {
                    response.disconnect();
                } catch (IOException e) {
                    logger().error("Error disconnecting response.", e);
                }
            }
        }
        return url.toString();
    }

    @Override
    public String putStorageDocument(InputStream stream, String type, String fileName) throws IOException, GeneralSecurityException {
        GenericUrl url = generateURLForDocument(fileName);
        HttpResponse response = null;
        try {
            initializeCloudStore();
            HttpContent content = new InputStreamContent(type, stream);
            HttpRequest request = buildHttpPutRequest(url, content);
            request.getHeaders().setCacheControl("private");
            response = request.execute();
            if (response.getStatusCode() != 200) {
                logger().error("Error storing contents: " + response.getStatusMessage());
            }
        } finally {
            if (null != response) {
                try {
                    response.disconnect();
                } catch (IOException e) {
                    logger().error("Error disconnecting response.", e);
                }
            }
        }
        return url.toString();
    }

    public Bucket getBucketMetadata() throws IOException, GeneralSecurityException {
        Storage client = StorageFactory.getService(sConfig.getPassword());

        // com.google.api.services.storage.Storage.Buckets.get()
        return client.buckets().get(sConfig.getBucket()).execute();
    }

    @Override
    public String postOntologyDocument(InputStream stream, String type, String fileName) throws IOException, GeneralSecurityException, URISyntaxException {
        InputStreamContent contentStream = new InputStreamContent(type, stream);
        StorageObject objectMetadata = new StorageObject()
                .setCacheControl("private")
                .setName(fileName)
                .setAcl(Arrays.asList(new ObjectAccessControl().setEntity("allUsers").setRole("READER")));
        Storage client = StorageFactory.getService(sConfig.getPassword());
        Storage.Objects.Insert insertRequest = client.objects().insert(
                sConfig.getBucket(), objectMetadata, contentStream);
        insertRequest.getRequestHeaders().setCacheControl("private");
        insertRequest.execute();
        return generateURLForDocument(fileName).toString();
    }

}