package com.winllc.pki.ra.service.validators;

import com.winllc.pki.ra.beans.form.DomainLinkToAccountRequestForm;
import com.winllc.acme.common.domain.Account;
import com.winllc.acme.common.domain.Domain;
import com.winllc.acme.common.repository.AccountRepository;
import com.winllc.acme.common.repository.DomainRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.List;
import java.util.Optional;

@Component
public class DomainLinkToAccountRequestValidator implements Validator {

    private static final Logger log = LogManager.getLogger(DomainLinkToAccountRequestValidator.class);

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private DomainRepository domainRepository;

    @Override
    public boolean supports(Class<?> clazz) {
        return DomainLinkToAccountRequestForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {

        DomainLinkToAccountRequestForm form = (DomainLinkToAccountRequestForm) target;

        Optional<Account> optionalAccount = accountRepository.findById(form.getAccountId());
        if(optionalAccount.isEmpty()){
            errors.rejectValue("accountId", "domainLinkToAccountRequest.invalidAccountId");
        }

        if(CollectionUtils.isEmpty(form.getRequestedDomainIds())){
            errors.rejectValue("requestedDomainIds", "domainLinkToAccountRequest.emptyDomainIds");
        }

        List<Long> requestedDomainIds = form.getRequestedDomainIds();
        for(Long domainId : requestedDomainIds){
            Optional<Domain> optionalDomain = domainRepository.findById(domainId);
            if(optionalDomain.isEmpty()){
                errors.rejectValue("requestedDomainIds", "domainLinkToAccountRequest.invalidDomainId");
            }
        }
    }
}
