package org.broadinstitute.consent.http.models.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class MailSendSearchTest {

  @Test
  void blank_text_is_no_search() {
    assertEquals(MailSendSearch.NONE, MailSendSearch.of("  ", List.of(4)));
    assertEquals(MailSendSearch.NONE, MailSendSearch.of(null, null));
  }

  @Test
  void text_matches_anywhere_with_wildcards_escaped() {
    assertEquals(
        new MailSendSearch("%50\\%\\_off\\\\%", List.of()), MailSendSearch.of(" 50%_off\\ ", null));
  }

  @Test
  void types_include_those_whose_name_reads_as_containing_the_text() {
    assertEquals(List.of(4, 34), MailSendSearch.of("vote reminder", List.of(4)).types());
  }
}
