package org.broadinstitute.consent.http.db.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class MailSendMapperTest {

  @Test
  void reads_numeric_dataset_aliases_as_duos_ids() throws Exception {
    ResultSet resultSet = mock(ResultSet.class);
    Array aliases = mock(Array.class);
    when(resultSet.getString("recipients")).thenReturn("[]");
    when(resultSet.getArray("dataset_aliases")).thenReturn(aliases);
    when(aliases.getArray()).thenReturn(new BigDecimal[] {BigDecimal.valueOf(45), BigDecimal.ONE});

    var send = new MailSendMapper().map(resultSet, null);

    assertEquals(List.of("DUOS-000045", "DUOS-000001"), send.datasetIdentifiers());
  }
}
