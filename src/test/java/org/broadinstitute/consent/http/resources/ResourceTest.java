package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.owasp.fileio.FileValidator;

@ExtendWith(MockitoExtension.class)
class ResourceTest {

  @Test
  void testValidateFileDetails() {
    Long maxSize = new FileValidator().getMaxFileUploadSize();
    Resource abstractResource = mock(Resource.class, Mockito.CALLS_REAL_METHODS);
    FormDataContentDisposition fileDetail = mock(FormDataContentDisposition.class);
    when(fileDetail.getFileName()).thenReturn("temp.txt");
    when(fileDetail.getSize()).thenReturn(maxSize);
    try {
      abstractResource.validateFileDetails(fileDetail);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void testValidateFileDetailsFileName() {
    Resource abstractResource = mock(Resource.class, Mockito.CALLS_REAL_METHODS);
    FormDataContentDisposition fileDetail = mock(FormDataContentDisposition.class);
    when(fileDetail.getFileName()).thenReturn("C:\\temp\\virus.exe");
    assertThrows(IllegalArgumentException.class, () -> {
      abstractResource.validateFileDetails(fileDetail);
    });
  }

  @Test
  void testValidateFileDetailsFileSize() {
    Long maxSize = new FileValidator().getMaxFileUploadSize();
    Resource abstractResource = mock(Resource.class, Mockito.CALLS_REAL_METHODS);
    FormDataContentDisposition fileDetail = mock(FormDataContentDisposition.class);
    when(fileDetail.getFileName()).thenReturn("temp.txt");
    when(fileDetail.getSize()).thenReturn(maxSize + 1);
    assertThrows(IllegalArgumentException.class, () -> {
      abstractResource.validateFileDetails(fileDetail);
    });
  }

}
