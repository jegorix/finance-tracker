package com.finance.tracker.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.Map;

public class RequestElapsedTimeConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        Map<String, String> mdcPropertyMap = event.getMDCPropertyMap();
        String requestStartTimeMillis = mdcPropertyMap.get(RequestLoggingContext.REQUEST_START_TIME_MILLIS);
        if (requestStartTimeMillis == null) {
            return "-";
        }

        try {
            long startTimeMillis = Long.parseLong(requestStartTimeMillis);
            long elapsedMillis = Math.max(0L, event.getTimeStamp() - startTimeMillis);
            return elapsedMillis + "ms";
        } catch (NumberFormatException exception) {
            return "-";
        }
    }
}
