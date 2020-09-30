package org.broadinstitute.consent.http.cloudstore;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
import com.google.api.services.storage.model.ObjectAccessControl;
import com.google.api.services.storage.model.StorageObject;
import com.google.inject.Inject;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import org.broadinstitute.consent.http.configurations.StoreConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use `GCSService` instead which uses standard google libraries for GCS access.
 */
@Deprecated
public class GCSStore implements CloudStore {

    private final static HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final StoreConfiguration sConfig;
    private HttpRequestFactory requestFactory;
    private GoogleCredential credential;

    @Inject
    public GCSStore(StoreConfiguration config) {
        sConfig = config;
        credential = null;
        requestFactory = null;
    }

    @Override
    public GenericUrl generateURLForDocument(String fileName) {
        return new GenericUrl(sConfig.getEndpoint() + sConfig.getBucket() + "/" + fileName);
    }

    @Override
    public boolean deleteStorageDocument(String documentUrl) {
        HttpResponse response = null;
        try {
            initializeCloudStore();
            HttpRequest request = buildHttpDeleteRequest(new GenericUrl(documentUrl));
            response = request.execute();
            return true;
        } catch (Exception e) {
            logger.warn("Error deleting document for the following url: " + documentUrl, e);
            return false;
        } finally {
            if (response != null) {
                try {
                    response.disconnect();
                } catch (IOException e) {
                    logger.error("Error disconnecting response.", e);
                }
            }
        }
    }

    @Override
    public HttpResponse getStorageDocument(String documentUrl) throws IOException {
        HttpResponse response;
        initializeCloudStore();
        HttpRequest request = buildHttpGetRequest(new GenericUrl(documentUrl));
        response = request.execute();
        return response;
    }

    @Override
    public String postStorageDocument(InputStream stream, String type, String fileName) throws IOException {
        return getDocumentUrl(stream, type, fileName);
    }

    @Override
    public String putStorageDocument(InputStream stream, String type, String fileName) throws IOException {
        return getDocumentUrl(stream, type, fileName);
    }

    @Override
    public String postOntologyDocument(InputStream stream, String type, String fileName) throws IOException, GeneralSecurityException {
        InputStreamContent contentStream = new InputStreamContent(type, stream);
        StorageObject objectMetadata = new StorageObject()
                .setCacheControl("private")
                .setName(fileName)
                .setAcl(Collections.singletonList(new ObjectAccessControl().setEntity("allUsers").setRole("READER")));
        Storage client = StorageFactory.getService(sConfig.getPassword());
        Storage.Objects.Insert insertRequest = client.objects().insert(
                sConfig.getBucket(), objectMetadata, contentStream);
        insertRequest.getRequestHeaders().setCacheControl("private");
        insertRequest.execute();
        return generateURLForDocument(fileName).toString();
    }

    private void initializeCloudStore() {
        if (credential == null) {
            credential = authorize();
            requestFactory = HTTP_TRANSPORT.createRequestFactory(credential);
        }
    }

    /**
     * Authorizes the application via service account to application data.
     */
    private GoogleCredential authorize() {
        GoogleCredential credential;
        try {
            credential = GoogleCredential.
                    fromStream(new FileInputStream(sConfig.getPassword())).
                    createScoped(Collections.singletonList(StorageScopes.DEVSTORAGE_FULL_CONTROL));
        } catch (Exception e) {
            logger.error("Error on GCS Store initialization. Service won't work: " + e);
            throw new RuntimeException(e);
        }
        return credential;
    }

    private HttpRequest buildHttpDeleteRequest(GenericUrl url) throws IOException {
        return requestFactory.buildDeleteRequest(url);
    }

    private HttpRequest buildHttpGetRequest(GenericUrl url) throws IOException {
        return requestFactory.buildGetRequest(url);
    }

    private HttpRequest buildHttpPutRequest(GenericUrl url, HttpContent content) throws IOException {
        return requestFactory.buildPutRequest(url, content);
    }

    private String getDocumentUrl(InputStream stream, String type, String fileName) throws IOException {
        GenericUrl url = generateURLForDocument(fileName);
        HttpResponse response = null;
        try {
            initializeCloudStore();
            HttpContent content = new InputStreamContent(type, stream);
            HttpRequest request = buildHttpPutRequest(url, content);
            request.getHeaders().setCacheControl("private");
            response = request.execute();
            if (response.getStatusCode() != 200) {
                logger.error("Error storing contents: " + response.getStatusMessage());
            }
        } finally {
            if (null != response) {
                try {
                    response.disconnect();
                } catch (IOException e) {
                    logger.error("Error disconnecting response.", e);
                }
            }
        }
        return url.toString();
    }

}
