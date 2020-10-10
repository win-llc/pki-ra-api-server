package com.winllc.pki.ra.service.validators;

import com.winllc.pki.ra.beans.form.AccountRequestForm;
import com.winllc.pki.ra.domain.AccountRequest;
import com.winllc.pki.ra.service.AccountRequestService;
import com.winllc.pki.ra.service.SecurityPolicyService;
import com.winllc.pki.ra.util.FormValidationUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class AccountRequestValidator implements Validator {

    private static final Logger log = LogManager.getLogger(AccountRequestValidator.class);

    private final SecurityPolicyService securityPolicyService;

    public AccountRequestValidator(SecurityPolicyService securityPolicyService) {
        this.securityPolicyService = securityPolicyService;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return AccountRequestForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        log.info("inside account request validator");

        AccountRequestForm form = (AccountRequestForm) target;

        boolean validEmail = FormValidationUtil.isValidEmailAddress(form.getAccountOwnerEmail());
        if(!validEmail){
            errors.rejectValue("accountOwnerEmail", "accountrequest.create.invalidAccountOwnerEmail");
        }

        if(StringUtils.isNotBlank(form.getSecurityPolicyServerProjectId())){
            try {
                boolean validProjectId = securityPolicyService.getAllProjectDetails()
                        .stream()
                        .anyMatch(d -> d.getProjectId().contentEquals(form.getSecurityPolicyServerProjectId()));

                if(!validProjectId){
                    errors.rejectValue("securityPolicyServerProjectId", "accountrequest.create.invalidSecurityPolicyProjectId");
                }
            } catch (Exception e) {
                log.error("Unable to locate policy server");
            }
        }
    }
}
