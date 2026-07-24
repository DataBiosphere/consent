package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.List;
import org.broadinstitute.consent.http.db.mapper.DataUseParser;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.util.TestAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class DataUseParserTest {

  @Test
  void testParseValidDataUse() {
    DataUseParser dataUseParser = new DataUseParser();
    DataUse test = new DataUseBuilder().setGeneralUse(true).build();
    DataUse dataUse = dataUseParser.parseDataUse(test.toString());
    assertNotNull(dataUse);
    assertEquals(test.getGeneralUse(), dataUse.getGeneralUse());
  }

  @Test
  void testParseInvalidDataUse() {
    String submittedDataUse = "sensitive invalid data use";
    Logger logger = (Logger) LoggerFactory.getLogger(DataUseParser.class);
    TestAppender appender = new TestAppender();
    appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
    logger.addAppender(appender);
    appender.start();
    DataUseParser dataUseParser = new DataUseParser();
    try {
      DataUse dataUse = dataUseParser.parseDataUse(submittedDataUse);
      assertNull(dataUse);
      List<ILoggingEvent> events = appender.getLoggedEvents();
      assertFalse(events.isEmpty());
      List<String> messages = events.stream().map(ILoggingEvent::getFormattedMessage).toList();
      assertFalse(messages.stream().anyMatch(message -> message.contains(submittedDataUse)));
      assertTrue(
          messages.stream()
              .anyMatch(
                  message ->
                      message.matches(
                          "Unable to parse data use string \\(length=26,"
                              + " sha256Prefix=[0-9a-f]{12}\\)")));
    } finally {
      appender.stop();
      logger.detachAppender(appender);
    }
  }

  @Test
  void testParseNullDataUse() {
    DataUseParser dataUseParser = new DataUseParser();
    DataUse dataUse = dataUseParser.parseDataUse(null);
    assertNull(dataUse);
  }

  @Test
  void testParseEmptyDataUse() {
    DataUseParser dataUseParser = new DataUseParser();
    DataUse dataUse = dataUseParser.parseDataUse("");
    assertNull(dataUse);
  }
}
