package com.finance.tracker.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class RequestElapsedTimeConverterTest {

    private final RequestElapsedTimeConverter converter = new RequestElapsedTimeConverter();

    @Test
    void shouldReturnElapsedTimeWhenRequestStartIsPresent() {
        LoggingEvent event = buildEvent(1_500L, Map.of(
                RequestLoggingContext.REQUEST_START_TIME_MILLIS,
                "1234"));

        assertThat(converter.convert(event)).isEqualTo("266ms");
    }

    @Test
    void shouldReturnDashWhenRequestStartIsMissing() {
        LoggingEvent event = buildEvent(1_500L, Map.of());

        assertThat(converter.convert(event)).isEqualTo("-");
    }

    @Test
    void shouldReturnDashWhenRequestStartIsInvalid() {
        LoggingEvent event = buildEvent(1_500L, Map.of(
                RequestLoggingContext.REQUEST_START_TIME_MILLIS,
                "invalid"));

        assertThat(converter.convert(event)).isEqualTo("-");
    }

    private LoggingEvent buildEvent(long timestamp, Map<String, String> mdcProperties) {
        LoggingEvent event = new LoggingEvent();
        event.setLoggerName(Logger.ROOT_LOGGER_NAME);
        event.setLevel(Level.INFO);
        event.setMessage("test");
        event.setTimeStamp(timestamp);
        event.setMDCPropertyMap(mdcProperties);
        event.setLoggerContextRemoteView(((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME))
                .getLoggerContext()
                .getLoggerContextRemoteView());
        return event;
    }
}
