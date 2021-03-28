package com.winllc.pki.ra.service.validators;

import com.winllc.pki.ra.beans.form.AccountRequestForm;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.AccountRequest;
import com.winllc.pki.ra.repository.AccountRepository;
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

import java.util.Optional;

@Component
public class AccountRequestValidator implements Validator {

    private static final Logger log = LogManager.getLogger(AccountRequestValidator.class);

    private final SecurityPolicyService securityPolicyService;
    @Autowired
    private AccountRepository accountRepository;

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

        if(StringUtils.isNotBlank(form.getProjectName())){
            Optional<Account> distinctByProjectName = accountRepository.findDistinctByProjectName(form.getProjectName());
            if(distinctByProjectName.isPresent()){
                errors.rejectValue("projectName", "accountrequest.create.invalidProjectName");
            }
        }else{
            errors.rejectValue("projectName", "accountrequest.create.emptyProjectName");
        }

        if(StringUtils.isNotBlank(form.getAccountOwnerEmail())) {
            boolean validEmail = FormValidationUtil.isValidEmailAddress(form.getAccountOwnerEmail());
            if (!validEmail) {
                errors.rejectValue("accountOwnerEmail", "accountrequest.create.invalidAccountOwnerEmail");
            }
        }else{
            errors.rejectValue("accountOwnerEmail", "accountrequest.create.emptyAccountOwnerEmail");
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
