package com.winllc.pki.ra.service.validators;

import com.winllc.pki.ra.beans.form.DomainLinkRequestDecisionForm;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.DomainLinkToAccountRequest;
import com.winllc.pki.ra.domain.DomainPolicy;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.DomainLinkToAccountRequestRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import javax.transaction.Transactional;
import java.util.Optional;
import java.util.Set;

@Component
public class DomainLinkRequestDecisionValidator implements Validator {

    private static final Logger log = LogManager.getLogger(DomainLinkRequestDecisionValidator.class);

    @Autowired
    private DomainLinkToAccountRequestRepository domainLinkToAccountRequestRepository;
    @Autowired
    private AccountRepository accountRepository;

    @Override
    public boolean supports(Class<?> clazz) {
        return DomainLinkRequestDecisionForm.class.isAssignableFrom(clazz);
    }

    @Override
    @Transactional
    public void validate(Object target, Errors errors) {

        DomainLinkRequestDecisionForm form = (DomainLinkRequestDecisionForm) target;

        Optional<DomainLinkToAccountRequest> optionalRequest = domainLinkToAccountRequestRepository.findById(form.getRequestId());

        if(optionalRequest.isEmpty()){
            errors.rejectValue("requestId", "domainLinkToAccountRequest.invalidRequestId");
        }else{
            DomainLinkToAccountRequest request = optionalRequest.get();

            Optional<Account> optionalAccount = accountRepository.findById(request.getAccountId());
            if(optionalAccount.isPresent()){
                Account account = optionalAccount.get();

                Set<DomainPolicy> accountDomainPolicies = account.getAccountDomainPolicies();

                Optional<DomainPolicy> domainExists = accountDomainPolicies.stream()
                        .filter(p -> request.getRequestedDomainIds().contains(p.getTargetDomain().getId()))
                        .findAny();

                if(domainExists.isPresent()){
                    errors.reject("domainLinkToAccountRequest.alreadyLinked");
                }
            }else{
                errors.reject("domainLinkToAccountRequest.noAccount");
            }
        }

        if(!form.getStatus().equals("approve") && !form.getStatus().equals("reject")){
            errors.rejectValue("status", "domainLinkToAccountRequest.invalidStatus");
        }
    }
}
