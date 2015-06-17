package org.genomebridge.consent.http.cloudstore;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

public interface CloudStore {

    // Updates should be handled delete -> create?

    public boolean deleteStorageDocument(String document) throws IOException, GeneralSecurityException;
    public HttpResponse getStorageDocument(GenericUrl documentUrl) throws IOException, GeneralSecurityException;
    public String putStorageDocument(String document, InputStream stream, FileType type) throws IOException, GeneralSecurityException;
    public String postStorageDocument(String document, InputStream stream, FileType type) throws IOException, GeneralSecurityException;
}
