package org.broadinstitute.consent.http.cloudstore;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;

public interface CloudStore {

    boolean deleteStorageDocument(String documentUrl) throws IOException, GeneralSecurityException;

    HttpResponse getStorageDocument(String documentUrl) throws IOException, GeneralSecurityException;

    String putStorageDocument(InputStream stream, String type, String fileName) throws IOException, GeneralSecurityException;

    String postStorageDocument(InputStream stream, String type, String fileName) throws IOException, GeneralSecurityException;

    GenericUrl generateURLForDocument(String fileName);

    String postOntologyDocument(InputStream stream, String type, String fileName) throws IOException, GeneralSecurityException, URISyntaxException;

}
