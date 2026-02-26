package org.broadinstitute.consent.http.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Collections;
import java.util.List;

public class TestAppender extends ListAppender<ILoggingEvent> {

  public void reset() {
    this.list.clear();
  }

  public int getSize() {
    return this.list.size();
  }

  public List<ILoggingEvent> getLoggedEvents() {
    return Collections.unmodifiableList(this.list);
  }
}
