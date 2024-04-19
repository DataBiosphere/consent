package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.broadinstitute.consent.http.db.mapper.DataUseParser;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

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
    DataUseParser dataUseParser = new DataUseParser();
    DataUse dataUse = dataUseParser.parseDataUse("test");
    assertNull(dataUse);
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
