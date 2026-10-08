package com.surendra.priorauth.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.surendra.priorauth.security.PhiMasker;

/**
 * Logback converter that scrubs member IDs and dates from every log message, so PHI cannot
 * leak into log files even if a developer logs a full object by mistake.
 */
public class PhiMaskingConverter extends MessageConverter {
    @Override
    public String convert(ILoggingEvent event) {
        return PhiMasker.scrub(super.convert(event));
    }
}
