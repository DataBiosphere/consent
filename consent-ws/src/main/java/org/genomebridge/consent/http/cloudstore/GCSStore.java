package org.genomebridge.consent.http.cloudstore;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.storage.StorageScopes;
import org.genomebridge.consent.http.configurations.StoreConfiguration;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.UUID;

public class GCSStore implements CloudStore {
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private final static HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    private StoreConfiguration sConfig;
    HttpRequestFactory requestFactory;
    GoogleCredential credential;

    protected Logger logger() {
        return Logger.getLogger("GCSStore");
    }

    public GCSStore(StoreConfiguration config) throws GeneralSecurityException, IOException {
        sConfig = config;
        credential = authorize();
        requestFactory = HTTP_TRANSPORT.createRequestFactory(credential);
    }

    private GenericUrl generateURLForDocument(String ext) {
        return new GenericUrl(sConfig.getEndpoint() + sConfig.getBucket() + "/" + UUID.randomUUID() + "." + ext);
    }

    /**
     * Authorizes the installed application to access user's protected data.
     */
    private GoogleCredential authorize() {
        GoogleCredential credential = new GoogleCredential();
        File file = new File(sConfig.getPassword());
        try {
            credential = new GoogleCredential.Builder()
                    .setTransport(HTTP_TRANSPORT)
                    .setJsonFactory(JSON_FACTORY)
                    .setServiceAccountId(sConfig.getUsername())
                    .setServiceAccountScopes(Collections.singletonList(StorageScopes.DEVSTORAGE_FULL_CONTROL))
                    .setServiceAccountPrivateKeyFromP12File(file)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
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
        HttpRequest request = requestFactory.buildGetRequest(url);
        return request;
    }

    @Override
    public HttpResponse getStorageDocument(String documentUrl) throws IOException, GeneralSecurityException {
        HttpResponse response;
        HttpRequest request = buildHttpGetRequest(new GenericUrl(documentUrl));
        response = request.execute();
        return response;
    }

    private HttpRequest buildHttpPutRequest(GenericUrl url, HttpContent content) throws IOException, GeneralSecurityException {
        HttpRequest request = requestFactory.buildPutRequest(url, content);
        return request;
    }

    @Override
    public String postStorageDocument(String documentID, InputStream stream, String type, String ext) throws IOException, GeneralSecurityException {
        GenericUrl url = generateURLForDocument(ext);
        HttpResponse response = null;
        try {
            HttpContent content = new InputStreamContent(type, stream);
            HttpRequest request = buildHttpPutRequest(url, content);
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
    public String putStorageDocument(String documentID, InputStream stream, String type, String ext) throws IOException, GeneralSecurityException {
        GenericUrl url = generateURLForDocument(ext);
        HttpResponse response = null;
        try {
            HttpContent content = new InputStreamContent(type, stream);
            HttpRequest request = buildHttpPutRequest(url, content);
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
}
