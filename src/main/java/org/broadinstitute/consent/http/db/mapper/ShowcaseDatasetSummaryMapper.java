package org.broadinstitute.consent.http.db.mapper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.enumeration.ShowcasePublicField;
import org.broadinstitute.consent.http.matching.TranslationUtil;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.ShowcaseDatasetSummary;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * Maps a {@code DatasetDAO.findShowcaseSummaries} row. Property values arrive as JSON objects keyed
 * by the allowlisted property name; anything the allowlist does not name is ignored.
 */
public class ShowcaseDatasetSummaryMapper
    implements RowMapper<ShowcaseDatasetSummary>, RowMapperHelper {

  private static final Set<String> DIRECT_ACCESS =
      Set.of(AccessManagement.OPEN.value(), AccessManagement.EXTERNAL.value());
  private static final Set<String> URL_SCHEMES = Set.of("http", "https");

  private final DataUseParser dataUseParser = new DataUseParser();

  @Override
  public ShowcaseDatasetSummary map(ResultSet rs, StatementContext ctx) throws SQLException {
    String accessManagement = rs.getString("access_management");
    JsonObject datasetProperties = parseObject(rs.getString("dataset_properties"));
    JsonObject studyProperties = parseObject(rs.getString("study_properties"));
    String accessUrl =
        accessManagement != null && DIRECT_ACCESS.contains(accessManagement)
            ? safeUrl(firstValue(datasetProperties, ShowcasePublicField.ACCESS_URL))
            : null;
    return new ShowcaseDatasetSummary(
        Dataset.parseAliasToIdentifier(rs.getInt("alias")),
        rs.getString("name"),
        accessManagement,
        rs.getString("dac_name"),
        TranslationUtil.summaryCodes(dataUseParser.parseDataUse(rs.getString("data_use"))),
        participantCount(firstValue(datasetProperties, ShowcasePublicField.PARTICIPANT_COUNT)),
        storedDate(rs.getObject("available_date", LocalDateTime.class)),
        accessUrl,
        StringUtils.trimToNull(firstValue(studyProperties, ShowcasePublicField.SPECIES)));
  }

  private static JsonObject parseObject(String json) {
    if (json == null) {
      return new JsonObject();
    }
    return JsonParser.parseString(json).getAsJsonObject();
  }

  private static String firstValue(JsonObject properties, ShowcasePublicField field) {
    for (String name : field.getSourceNames()) {
      JsonElement value = properties.get(name);
      if (value != null && !value.isJsonNull()) {
        return value.getAsString();
      }
    }
    return null;
  }

  private static Integer participantCount(String value) {
    if (value == null) {
      return null;
    }
    try {
      int count = PropertyType.coerceToNumber(value.trim()).intValue();
      return count >= 0 ? count : null;
    } catch (IllegalArgumentException _) {
      return null;
    }
  }

  /** The date part of a stored timestamp, unaffected by the JVM time zone. */
  private static String storedDate(LocalDateTime timestamp) {
    return timestamp == null ? null : timestamp.toLocalDate().toString();
  }

  private static String safeUrl(String value) {
    String url = StringUtils.trimToNull(value);
    if (url == null) {
      return null;
    }
    try {
      URI uri = new URI(url);
      if (uri.getScheme() == null
          || !URL_SCHEMES.contains(uri.getScheme().toLowerCase(Locale.ROOT))
          || StringUtils.isBlank(uri.getHost())
          || uri.getRawUserInfo() != null) {
        return null;
      }
      return url;
    } catch (URISyntaxException _) {
      return null;
    }
  }
}
