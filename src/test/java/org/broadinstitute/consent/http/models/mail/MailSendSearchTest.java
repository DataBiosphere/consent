package org.broadinstitute.consent.http.models.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
        new MailSendSearch("%50\\%\\_off\\\\%", null, List.of()),
        MailSendSearch.of(" 50%_off\\ ", null));
  }

  @Test
  void types_include_those_whose_name_holds_the_text_as_whole_words() {
    assertEquals(List.of(4, 34), MailSendSearch.of("Vote Reminder", List.of(4)).types());
    assertEquals(List.of(), MailSendSearch.of("al", null).types());
  }

  @Test
  void a_duos_id_names_its_alias_padded_or_not() {
    assertEquals(123L, MailSendSearch.of("duos-123", null).alias());
    assertEquals(123L, MailSendSearch.of("DUOS-000123", null).alias());
    assertNull(MailSendSearch.of("DUOS-", null).alias());
  }
}
