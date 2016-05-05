package org.broadinstitute.consent.http.cloudstore;

import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.storage.StorageScopes;
import org.apache.log4j.Logger;
import org.broadinstitute.consent.http.configurations.StoreConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.security.GeneralSecurityException;
import java.util.Collections;

public class GCSStore implements CloudStore {

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private final static HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    private final StoreConfiguration sConfig;
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

    @Override
    public GenericUrl generateURLForDocument(String fileName) {
        return new GenericUrl(sConfig.getEndpoint() + sConfig.getBucket() + "/" + fileName);
    }

    /**
     * Authorizes the installed application to access user's protected data.
     */
    private GoogleCredential authorize() {
        GoogleCredential credential = new GoogleCredential();
        try {
            InputStream inputStream = new FileInputStream(sConfig.getPassword());
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(inputStream));
            String privateKeyPem = (String) clientSecrets.get("private_key");
            String clientEmail = (String) clientSecrets.get("client_email");
            String privateKeyId = (String) clientSecrets.get("private_key_id");
            File privateKeyPemFile = createKeyPemFile(privateKeyPem);
            credential = new GoogleCredential.Builder()
                    .setTransport(HTTP_TRANSPORT)
                    .setJsonFactory(JSON_FACTORY)
                    .setServiceAccountId(clientEmail)
                    .setServiceAccountScopes(Collections.singletonList(StorageScopes.DEVSTORAGE_FULL_CONTROL))
                    .setServiceAccountPrivateKeyFromPemFile(privateKeyPemFile)
                    .setServiceAccountPrivateKeyId(privateKeyId)
                    .build();

        } catch (Exception e) {
            logger().error("Error on GCS Store initialization. Service won't work: " + e);
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
    public String postStorageDocument(InputStream stream, String type, String fileName) throws IOException, GeneralSecurityException {
        GenericUrl url = generateURLForDocument(fileName);
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
    public String putStorageDocument(InputStream stream, String type, String fileName) throws IOException, GeneralSecurityException {
        GenericUrl url = generateURLForDocument(fileName);
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

    private File createKeyPemFile(String privateKeyPem) throws IOException {
        File privateKeyPemFile = File.createTempFile("privateKeyPem", "txt");
        FileWriter fileWriter = new FileWriter(privateKeyPemFile);
        fileWriter.write(privateKeyPem);
        fileWriter.flush();
        fileWriter.close();
        return privateKeyPemFile;
    }
}