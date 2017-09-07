package org.broadinstitute.consent.http.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.getsentry.raven.logback.SentryAppender;
import io.dropwizard.logging.AppenderFactory;
import io.dropwizard.logging.async.AsyncAppenderFactory;
import io.dropwizard.logging.filter.LevelFilterFactory;
import io.dropwizard.logging.layout.LayoutFactory;

/**
 * See https://gist.github.com/ajmath/e9f90c29cd224653c218
 *
 * Requires the following configuration:
 * logging:
 *   appenders:
 *     - type: sentry
 */
@JsonTypeName("sentry")
public class SentryLogger implements AppenderFactory {

    @Override
    public Appender build(LoggerContext context, String applicationName, LayoutFactory layoutFactory, LevelFilterFactory levelFilterFactory, AsyncAppenderFactory asyncAppenderFactory) {
        final SentryAppender appender = new SentryAppender();
        appender.setName("sentry");
        appender.setContext(context);
        return appender;
    }

}
