package com.winllc.pki.ra.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

public class DateUtil {

    private static final Logger log = LogManager.getLogger(DateUtil.class);

    public static Optional<LocalDateTime> isoTimestampToLocalDateTime(String timestamp){
        if(StringUtils.isNotBlank(timestamp)){
            try {
                Instant ld = Instant.parse(timestamp);
                return Optional.of(LocalDateTime.ofInstant(ld, ZoneId.systemDefault()));
            }catch (Exception e){
                log.debug("Could not parse", e);
            }
        }
        return Optional.empty();
    }
}
