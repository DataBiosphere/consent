package org.broadinstitute.consent.http.db.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.JsonSyntaxException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.jdbi.v3.core.result.RowView;
import org.junit.jupiter.api.Test;

class RowMapperHelperTest {

  /** Free text the read path used to destroy: once unescaped, the quote ended the string early. */
  private static final String QUOTED_FREE_TEXT =
      "PI said \"no resale\" applies; see C:\\temp\\notes.";

  private static final String COLUMN = "user_id";

  private final RowMapperHelper helper = new RowMapperHelper() {};

  private static ResultSet resultSetWithColumn(String columnName) throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    ResultSetMetaData metaData = mock(ResultSetMetaData.class);
    when(resultSet.getMetaData()).thenReturn(metaData);
    when(metaData.getColumnCount()).thenReturn(1);
    when(metaData.getColumnName(1)).thenReturn(columnName);
    return resultSet;
  }

  @Test
  void testTranslateKeepsQuotedFreeText() {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setRus(QUOTED_FREE_TEXT);
    data.setNonTechRus(QUOTED_FREE_TEXT);

    DataAccessRequestData parsed = helper.translate(data.toString());

    assertEquals(QUOTED_FREE_TEXT, parsed.getRus());
    assertEquals(QUOTED_FREE_TEXT, parsed.getNonTechRus());
  }

  @Test
  void testTranslateReturnsNullWithoutAValue() {
    assertNull(helper.translate(null));
  }

  @Test
  void testTranslateRejectsMalformedJson() {
    assertThrows(JsonSyntaxException.class, () -> helper.translate("{\"rus\": \"unterminated"));
  }

  @Test
  void testHasColumnForRowView() {
    RowView rowView = mock(RowView.class);
    when(rowView.getColumn(COLUMN, Integer.class)).thenReturn(1);
    assertTrue(helper.hasColumn(rowView, COLUMN, Integer.class));
  }

  @Test
  void testHasColumnForRowViewWithoutAValue() {
    RowView rowView = mock(RowView.class);
    when(rowView.getColumn(COLUMN, Integer.class)).thenReturn(null);
    assertFalse(helper.hasColumn(rowView, COLUMN, Integer.class));
  }

  @Test
  void testHasColumnForRowViewWhenTheColumnIsAbsent() {
    RowView rowView = mock(RowView.class);
    when(rowView.getColumn(COLUMN, Integer.class)).thenThrow(new IllegalArgumentException());
    assertFalse(helper.hasColumn(rowView, COLUMN, Integer.class));
  }

  @Test
  void testHasNonZeroColumnForRowView() {
    RowView rowView = mock(RowView.class);
    when(rowView.getColumn(COLUMN, Integer.class)).thenReturn(7);
    assertTrue(helper.hasNonZeroColumn(rowView, COLUMN));
  }

  @Test
  void testHasNonZeroColumnForRowViewWithZero() {
    RowView rowView = mock(RowView.class);
    when(rowView.getColumn(COLUMN, Integer.class)).thenReturn(0);
    assertFalse(helper.hasNonZeroColumn(rowView, COLUMN));
  }

  @Test
  void testHasNonZeroColumnForRowViewWithoutAValue() {
    RowView rowView = mock(RowView.class);
    when(rowView.getColumn(COLUMN, Integer.class)).thenReturn(null);
    assertFalse(helper.hasNonZeroColumn(rowView, COLUMN));
  }

  @Test
  void testHasNonZeroColumnForRowViewWhenTheColumnIsAbsent() {
    RowView rowView = mock(RowView.class);
    when(rowView.getColumn(COLUMN, Integer.class)).thenThrow(new IllegalArgumentException());
    assertFalse(helper.hasNonZeroColumn(rowView, COLUMN));
  }

  @Test
  void testHasOptionalColumn() {
    RowView rowView = mock(RowView.class);
    when(rowView.getColumn(COLUMN, String.class)).thenReturn("value");
    assertEquals(Optional.of("value"), helper.hasOptionalColumn(rowView, COLUMN, String.class));
  }

  @Test
  void testHasOptionalColumnWhenTheColumnIsAbsent() {
    RowView rowView = mock(RowView.class);
    when(rowView.getColumn(COLUMN, String.class)).thenThrow(new IllegalArgumentException());
    assertEquals(Optional.empty(), helper.hasOptionalColumn(rowView, COLUMN, String.class));
  }

  @Test
  void testHasColumnForResultSetMatchesCaseInsensitively() throws SQLException {
    assertTrue(helper.hasColumn(resultSetWithColumn("USER_ID"), COLUMN));
  }

  @Test
  void testHasColumnForResultSetWhenTheColumnIsAbsent() throws SQLException {
    assertFalse(helper.hasColumn(resultSetWithColumn("other"), COLUMN));
  }

  @Test
  void testHasNonZeroColumnForResultSet() throws SQLException {
    ResultSet resultSet = resultSetWithColumn(COLUMN);
    when(resultSet.getInt(COLUMN)).thenReturn(3);
    assertTrue(helper.hasNonZeroColumn(resultSet, COLUMN));
  }

  @Test
  void testHasNonZeroColumnForResultSetWithZero() throws SQLException {
    ResultSet resultSet = resultSetWithColumn(COLUMN);
    when(resultSet.getInt(COLUMN)).thenReturn(0);
    assertFalse(helper.hasNonZeroColumn(resultSet, COLUMN));
  }

  @Test
  void testHasNonZeroColumnForResultSetWhenTheColumnIsAbsent() throws SQLException {
    assertFalse(helper.hasNonZeroColumn(resultSetWithColumn("other"), COLUMN));
  }

  @Test
  void testSetStringFieldValue() throws SQLException {
    ResultSet resultSet = resultSetWithColumn(COLUMN);
    when(resultSet.getString(COLUMN)).thenReturn("value");
    AtomicReference<String> set = new AtomicReference<>();

    helper.setStringFieldValue(resultSet, COLUMN, set::set);

    assertEquals("value", set.get());
  }

  @Test
  void testSetStringFieldValueSkipsAnAbsentColumn() throws SQLException {
    AtomicReference<String> set = new AtomicReference<>();

    helper.setStringFieldValue(resultSetWithColumn("other"), COLUMN, set::set);

    assertNull(set.get());
  }

  @Test
  void testSetNonZeroFieldValue() throws SQLException {
    ResultSet resultSet = resultSetWithColumn(COLUMN);
    when(resultSet.getInt(COLUMN)).thenReturn(9);
    AtomicInteger set = new AtomicInteger();

    helper.setNonZeroFieldValue(resultSet, COLUMN, set::set);

    assertEquals(9, set.get());
  }

  @Test
  void testSetNonZeroFieldValueSkipsAZeroColumn() throws SQLException {
    ResultSet resultSet = resultSetWithColumn(COLUMN);
    when(resultSet.getInt(COLUMN)).thenReturn(0);
    AtomicInteger set = new AtomicInteger(-1);

    helper.setNonZeroFieldValue(resultSet, COLUMN, set::set);

    assertEquals(-1, set.get());
  }

  @Test
  void testSetDateFieldValue() throws SQLException {
    java.sql.Date date = java.sql.Date.valueOf("2026-08-26");
    ResultSet resultSet = resultSetWithColumn(COLUMN);
    when(resultSet.getDate(COLUMN)).thenReturn(date);
    AtomicReference<Date> set = new AtomicReference<>();

    helper.setDateFieldValue(resultSet, COLUMN, set::set);

    assertEquals(date, set.get());
  }

  @Test
  void testSetDateFieldValueSkipsAnAbsentColumn() throws SQLException {
    AtomicReference<Date> set = new AtomicReference<>();

    helper.setDateFieldValue(resultSetWithColumn("other"), COLUMN, set::set);

    assertNull(set.get());
  }
}
