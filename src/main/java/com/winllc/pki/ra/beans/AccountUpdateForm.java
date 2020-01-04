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
    private List<PocFormEntry> pocEmails;
    private boolean acmeRequireHttpValidation;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<PocFormEntry> getPocEmails() {
        return pocEmails;
    }

    public void setPocEmails(List<PocFormEntry> pocEmails) {
        this.pocEmails = pocEmails;
    }

    public boolean isAcmeRequireHttpValidation() {
        return acmeRequireHttpValidation;
    }

    public void setAcmeRequireHttpValidation(boolean acmeRequireHttpValidation) {
        this.acmeRequireHttpValidation = acmeRequireHttpValidation;
    }

    public boolean isValid(){
        boolean valid;
        if(!CollectionUtils.isEmpty(pocEmails)){
            Pattern pattern = Pattern.compile(VALID_EMAIL_REGEX);
            valid = pocEmails.stream()
                    .allMatch(p -> pattern.matcher(p.getEmail()).matches());
        }else{
            valid = true;
        }
        return valid;
    }
}
