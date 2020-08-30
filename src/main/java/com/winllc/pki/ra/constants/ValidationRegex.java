package com.winllc.pki.ra.constants;

import java.util.regex.Pattern;

public class ValidationRegex {
    public static String FQDN_VALIDATION_REGEX = "(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";
    public static Pattern FQDN_VALIDATION_PATTERN = Pattern.compile(FQDN_VALIDATION_REGEX);
}
