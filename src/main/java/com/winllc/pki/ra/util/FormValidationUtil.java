package com.winllc.pki.ra.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormValidationUtil {

    private static final String EMAIL_VALIDATION_REGEX = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$";

    public static boolean isValidEmailAddress(String email){
        return verifyWithRegex(email, EMAIL_VALIDATION_REGEX);
    }

    private static boolean verifyWithRegex(String val, String regex){
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(val);
        return matcher.matches();
    }
}
