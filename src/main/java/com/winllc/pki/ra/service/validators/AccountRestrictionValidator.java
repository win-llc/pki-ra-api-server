package com.winllc.pki.ra.service.validators;

import com.winllc.pki.ra.beans.form.AccountRestrictionForm;
import com.winllc.acme.common.constants.AccountRestrictionAction;
import com.winllc.acme.common.constants.AccountRestrictionType;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class AccountRestrictionValidator implements Validator {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
    private static final Logger log = LogManager.getLogger(AccountRestrictionValidator.class);

    @Override
    public boolean supports(Class<?> clazz) {
        return AccountRestrictionForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {

        AccountRestrictionForm form = (AccountRestrictionForm) target;

        if (StringUtils.isNotBlank(form.getType())) {
            try {
                AccountRestrictionType.valueOf(form.getType());
            } catch (IllegalArgumentException e) {
                errors.rejectValue("type", "accountrestriction.create.invalidtype");
            }
        }

        if (StringUtils.isNotBlank(form.getAction())) {
            try {
                AccountRestrictionAction.valueOf(form.getAction());
            } catch (IllegalArgumentException e) {
                errors.rejectValue("action", "accountrestriction.create.invalidaction");
            }
        }

        try {
            LocalDateTime.from(formatter.parse(form.getDueBy()));
        }catch (Exception e){
            errors.rejectValue("dueBy", "accountrestriction.create.invaliddueby");
        }
    }
}
