package com.winllc.pki.ra.service.validators;

import com.winllc.pki.ra.beans.form.DomainForm;
import com.winllc.pki.ra.beans.form.ServerEntryForm;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.ServerEntry;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.ServerEntryRepository;
import com.winllc.pki.ra.util.FormValidationUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Optional;

@Component
public class ServerEntryValidator implements Validator {

    private static final Logger log = LogManager.getLogger(ServerEntryValidator.class);

    private final AccountRepository accountRepository;
    private final ServerEntryRepository serverEntryRepository;

    public ServerEntryValidator(AccountRepository accountRepository, ServerEntryRepository serverEntryRepository) {
        this.accountRepository = accountRepository;
        this.serverEntryRepository = serverEntryRepository;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return ServerEntryForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {

        ServerEntryForm form = (ServerEntryForm) target;

        boolean isEdit = false;
        if(form.getId() != null){
            Optional<ServerEntry> serverOptional = serverEntryRepository.findById(form.getId());
            isEdit = serverOptional.isPresent();
        }

        if(!isEdit) {
            Optional<Account> optionalAccount = accountRepository.findById(form.getAccountId());
            if (optionalAccount.isEmpty()) {
                errors.rejectValue("accountId", "serverEntry.invalidAccountId");
            }
        }

        if(!isEdit) {
            if (!FormValidationUtil.isValidFqdn(form.getFqdn())) {
                errors.rejectValue("alternateDnsValues", "serverEntry.invalidFqdn");
            }
        }

        if(!CollectionUtils.isEmpty(form.getAlternateDnsValues())){
            for(String fqdn : form.getAlternateDnsValues()){
                boolean valid = FormValidationUtil.isValidFqdn(fqdn);
                if(!valid) {
                    errors.rejectValue("alternateDnsValues", "serverEntry.invalidDns");
                }
            }
        }

        if(StringUtils.isNotBlank(form.getOpenidClientRedirectUrl())){
            if(!FormValidationUtil.isValidFqdn(form.getOpenidClientRedirectUrl())){
                errors.rejectValue("openidClientRedirectUrl", "serverEntry.invalidOpenidClientRedirectUrl");
            }
        }
    }
}
