package com.winllc.pki.ra.service.validators;

import com.winllc.pki.ra.beans.form.DomainLinkRequestDecisionForm;
import com.winllc.pki.ra.domain.DomainLinkToAccountRequest;
import com.winllc.pki.ra.repository.DomainLinkToAccountRequestRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Optional;

@Component
public class DomainLinkRequestDecisionValidator implements Validator {

    private static final Logger log = LogManager.getLogger(DomainLinkRequestDecisionValidator.class);

    @Autowired
    private DomainLinkToAccountRequestRepository domainLinkToAccountRequestRepository;

    @Override
    public boolean supports(Class<?> clazz) {
        return DomainLinkRequestDecisionForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {

        DomainLinkRequestDecisionForm form = (DomainLinkRequestDecisionForm) target;

        Optional<DomainLinkToAccountRequest> optionalRequest = domainLinkToAccountRequestRepository.findById(form.getRequestId());

        if(optionalRequest.isEmpty()){
            errors.rejectValue("requestId", "domainLinkToAccountRequest.invalidRequestId");
        }

        if(!form.getStatus().equals("approve") && !form.getStatus().equals("reject")){
            errors.rejectValue("status", "domainLinkToAccountRequest.invalidStatus");
        }
    }
}
