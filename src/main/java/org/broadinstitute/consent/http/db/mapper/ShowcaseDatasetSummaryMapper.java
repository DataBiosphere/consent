package org.broadinstitute.consent.http.db.mapper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 * by the lower-cased allowlisted property name; anything the allowlist does not name is ignored.
 */
public class ShowcaseDatasetSummaryMapper
    implements RowMapper<ShowcaseDatasetSummary>, RowMapperHelper {

  private static final Set<String> DIRECT_ACCESS =
      Set.of(AccessManagement.OPEN.value(), AccessManagement.EXTERNAL.value());
  private static final Set<String> URL_SCHEMES = Set.of("http", "https");
  // A host with no brackets, colons or "@", then an optional port.
  private static final Pattern REGISTRY_AUTHORITY =
      Pattern.compile("([^:\\[\\]@]+)(?::(\\d{1,5}))?");
  private static final Pattern ASCII_HOST =
      Pattern.compile(
          "(?i)[a-z0-9_](?:[a-z0-9_-]*[a-z0-9_])?(?:\\.[a-z0-9_](?:[a-z0-9_-]*[a-z0-9_])?)*\\.?");
  private static final Pattern NUMERIC_LAST_LABEL = Pattern.compile("(?:.*\\.)?\\d+\\.?");

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
      JsonElement value = properties.get(name.toLowerCase(Locale.ROOT));
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

  /**
   * An absolute http(s) URL whose authority is a host and an optional port from 1 to 65535, with no
   * user info. {@link URI#getHost()} is null for a host with an underscore or a non-ASCII
   * character, so that authority is parsed here. A non-ASCII host is returned in its ASCII
   * (punycode) form, so a homograph or a bidi control never reaches the page as text.
   */
  private static String safeUrl(String value) {
    String url = StringUtils.trimToNull(value);
    if (url == null) {
      return null;
    }
    try {
      URI uri = new URI(url);
      String scheme = uri.getScheme();
      String authority = uri.getRawAuthority();
      if (scheme == null
          || !URL_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))
          || authority == null) {
        return null;
      }
      if (uri.getHost() != null) {
        return uri.getRawUserInfo() == null && validPort(uri.getPort()) ? url : null;
      }
      Matcher matcher = REGISTRY_AUTHORITY.matcher(authority);
      if (!matcher.matches()) {
        return null;
      }
      String host = IDN.toASCII(matcher.group(1));
      String port = matcher.group(2);
      if (!ASCII_HOST.matcher(host).matches()
          || NUMERIC_LAST_LABEL.matcher(host).matches()
          || (port != null && !validPort(Integer.parseInt(port)))) {
        return null;
      }
      String asciiAuthority = port == null ? host : host + ":" + port;
      // The raw authority follows "scheme://" verbatim, so the rest of the URL is unchanged.
      return scheme
          + "://"
          + asciiAuthority
          + url.substring(scheme.length() + "://".length() + authority.length());
    } catch (URISyntaxException | IllegalArgumentException _) {
      return null;
    }
  }

  /** {@link URI#getPort()} gives -1 when there is no port. */
  private static boolean validPort(int port) {
    return port == -1 || (port >= 1 && port <= 65535);
  }
}
