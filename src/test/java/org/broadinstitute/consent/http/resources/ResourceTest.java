package org.broadinstitute.consent.http.resources;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.fileio.FileValidator;

public class ResourceTest {

    @Test
    public void testValidateFileDetails() {
        Long maxSize = new FileValidator().getMaxFileUploadSize();
        Resource abstractResource = mock(Resource.class, Mockito.CALLS_REAL_METHODS);
        FormDataContentDisposition fileDetail = mock(FormDataContentDisposition.class);
        when(fileDetail.getFileName()).thenReturn("temp.txt");
        when(fileDetail.getSize()).thenReturn(maxSize);
        try {
            abstractResource.validateFileDetails(fileDetail);
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
    }

    @Test
    public void testValidateFileDetailsFileName() {
        Resource abstractResource = mock(Resource.class, Mockito.CALLS_REAL_METHODS);
        FormDataContentDisposition fileDetail = mock(FormDataContentDisposition.class);
        when(fileDetail.getFileName()).thenReturn("C:\\temp\\virus.exe");
        try {
            abstractResource.validateFileDetails(fileDetail);
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testValidateFileDetailsFileSize() {
        Long maxSize = new FileValidator().getMaxFileUploadSize();
        Resource abstractResource = mock(Resource.class, Mockito.CALLS_REAL_METHODS);
        FormDataContentDisposition fileDetail = mock(FormDataContentDisposition.class);
        when(fileDetail.getFileName()).thenReturn("temp.txt");
        when(fileDetail.getSize()).thenReturn(maxSize + 1);
        try {
            abstractResource.validateFileDetails(fileDetail);
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalArgumentException);
        }
    }

}
