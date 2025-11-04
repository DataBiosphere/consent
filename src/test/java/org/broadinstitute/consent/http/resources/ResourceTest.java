package org.broadinstitute.consent.http.resources;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.broadinstitute.consent.http.models.Error;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementExceptions;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.owasp.fileio.FileValidator;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

@ExtendWith(MockitoExtension.class)
class ResourceTest {

  @Test
  void testUnableToExecuteExceptionGeneric() {
    var error = new UnableToExecuteStatementException("generic error");
    var result = Resource.unableToExecuteExceptionHandler(error);
    var entity = (Error) result.getEntity();
    assertThat(result.getStatus(), is(500));
    assertThat(entity.message(), is("Database error"));
  }

  @Test
  void testUnableToExecuteExceptionDatabaseConflict() {
    PSQLException psqlException =
        new PSQLException(
            "duplicate key value violates unique constraint", PSQLState.UNIQUE_VIOLATION);
    StatementContext ctx = mock(StatementContext.class);
    StatementExceptions exceptions = mock(StatementExceptions.class);
    when(ctx.getConfig(StatementExceptions.class)).thenReturn(exceptions);
    UnableToExecuteStatementException exception =
        new UnableToExecuteStatementException("Failed to execute statement", psqlException, ctx);

    var result = Resource.unableToExecuteExceptionHandler(exception);
    var entity = (Error) result.getEntity();
    assertThat(result.getStatus(), is(409));
    assertThat(entity.message(), is("Database conflict"));
  }

  @Test
  void testUnableToExecuteExceptionInvalidByteSequence() {
    PSQLState psqlState = mock(PSQLState.class);
    // PSQLState is missing the enum constant 22021 for invalid byte sequence but returns it so we
    // mock it
    when(psqlState.getState()).thenReturn("22021");
    PSQLException psqlException =
        new PSQLException("invalid byte sequence for encoding \"UTF8\": 0x00", psqlState);
    StatementContext ctx = mock(StatementContext.class);
    StatementExceptions exceptions = mock(StatementExceptions.class);
    when(ctx.getConfig(StatementExceptions.class)).thenReturn(exceptions);
    UnableToExecuteStatementException exception =
        new UnableToExecuteStatementException("Failed to execute statement", psqlException, ctx);

    var result = Resource.unableToExecuteExceptionHandler(exception);
    var entity = (Error) result.getEntity();
    assertThat(result.getStatus(), is(400));
    assertThat(entity.message(), is("Invalid byte sequence"));
  }

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
    assertThrows(
        IllegalArgumentException.class,
        () -> {
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
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          abstractResource.validateFileDetails(fileDetail);
        });
  }
}
