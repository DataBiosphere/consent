package org.broadinstitute.consent.http.util;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogHandler extends Handler {
    private Level lastLevel = Level.FINEST;

    public LogHandler(Level level) {
        setLevel(level);
    }

    public Level checkLevel() {
        return lastLevel;
    }

    public void publish(LogRecord record) {
        lastLevel = record.getLevel();
    }

    public void close(){}
    public void flush(){}
}
