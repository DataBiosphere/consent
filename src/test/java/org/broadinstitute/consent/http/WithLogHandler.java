package org.broadinstitute.consent.http;

import org.broadinstitute.consent.http.resources.DataRequestVoteResource;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public interface WithLogHandler {
    class LogHandler extends Handler {
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

    default LogHandler createLogHandler(String className) {
        Logger logger = Logger.getLogger(className);
        LogHandler handler = new LogHandler(Level.ALL);
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.ALL);
        return handler;
    }
}
