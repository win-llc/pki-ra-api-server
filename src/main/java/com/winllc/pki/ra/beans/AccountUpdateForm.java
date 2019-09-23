package com.winllc.pki.ra.beans;

import org.h2.util.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AccountUpdateForm implements ValidForm {
    //TODO

    private static final String VALID_EMAIL_REGEX = "^(.+)@(.+)$";

    private Long id;
    private List<String> pocEmails;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<String> getPocEmails() {
        return pocEmails;
    }

    public void setPocEmails(List<String> pocEmails) {
        this.pocEmails = pocEmails;
    }

    public boolean isValid(){
        boolean valid;
        if(!CollectionUtils.isEmpty(pocEmails)){
            Pattern pattern = Pattern.compile(VALID_EMAIL_REGEX);
            valid = pocEmails.stream()
                    .allMatch(p -> pattern.matcher(p).matches());
        }else{
            valid = false;
        }
        return valid;
    }
}
