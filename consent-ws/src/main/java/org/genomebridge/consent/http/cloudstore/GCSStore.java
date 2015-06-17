package org.genomebridge.consent.http.cloudstore;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;

public class GCSStore implements CloudStore {
    private static final String APPLICATION_NAME = "CONSENT-CI";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private final static HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    private StoreConfiguration sConfig;
    private Storage client;
    private static HttpTransport httpTransport;
    HttpRequestFactory requestFactory;
    GoogleCredential credential;

    protected Logger logger() {
        return Logger.getLogger("GCSStore");
    }

    public GCSStore(StoreConfiguration config) throws GeneralSecurityException, IOException {
        sConfig = config;

        // Authorization.
        credential = authorize();

        httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        // Set up global Storage instance.
        requestFactory = httpTransport.createRequestFactory(credential);

        client = new Storage.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME).build();

    }

    private GenericUrl getURLForDocument(String documentID){
       return new GenericUrl(sConfig.getEndpoint() + sConfig.getBucket() + "/" + documentID);
    }

    /** Authorizes the installed application to access user's protected data. */
    private GoogleCredential authorize(){
        // Load client secrets.
        GoogleCredential credential = new GoogleCredential();
        // Temporary to access the p12 file
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        File file = new File(cl.getResource("google.p12").getFile());
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
    public boolean deleteStorageDocument(String documentID) throws IOException, GeneralSecurityException {
        HttpResponse response = null;
        try {
            HttpRequest request = buildHttpDeleteRequest(getURLForDocument(documentID));
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
    public HttpResponse getStorageDocument(GenericUrl documentUrl) throws IOException, GeneralSecurityException {
        HttpResponse response;
        HttpRequest request = buildHttpGetRequest(documentUrl);
        response = request.execute();
        return response;
    }

    private HttpRequest buildHttpPutRequest(GenericUrl url, HttpContent content) throws IOException, GeneralSecurityException {
        HttpRequest request = requestFactory.buildPutRequest(url, content);
        return request;
    }

    @Override
    public String postStorageDocument(String documentID, InputStream stream, FileType type) throws IOException, GeneralSecurityException {
        GenericUrl url = getURLForDocument(documentID);
        HttpResponse response = null;
        try {
            HttpContent content = new InputStreamContent(type.toString(), stream);
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
    public String putStorageDocument(String documentID, InputStream stream, FileType type) throws IOException, GeneralSecurityException{
        GenericUrl url = getURLForDocument(documentID);
        HttpResponse response = null;
        try {
            HttpContent content = new InputStreamContent(type.toString(), stream);
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
