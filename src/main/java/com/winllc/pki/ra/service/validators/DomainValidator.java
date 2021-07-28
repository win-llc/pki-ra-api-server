package com.winllc.pki.ra.service.validators;

import com.winllc.pki.ra.beans.form.DomainForm;
import com.winllc.pki.ra.beans.form.DomainLinkToAccountRequestForm;
import com.winllc.acme.common.domain.Domain;
import com.winllc.acme.common.repository.DomainRepository;
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
public class DomainValidator implements Validator {

    private static final Logger log = LogManager.getLogger(DomainValidator.class);

    @Autowired
    private DomainRepository domainRepository;

    @Override
    public boolean supports(Class<?> clazz) {
        return DomainForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {

        DomainForm form = (DomainForm) target;

        if(StringUtils.isBlank(form.getBase())){
            errors.rejectValue("base", "domain.emptyBase");
        }else if(form.getBase().contains(".") || !FormValidationUtil.isValidFqdn(form.getBase())){
            errors.rejectValue("base", "domain.invalidBase");
        }

        if(form.getParentDomainId() != null){
            Optional<Domain> optionalDomain = domainRepository.findById(form.getParentDomainId());
            if(optionalDomain.isEmpty()){
                errors.rejectValue("parentDomainId", "domain.invalidParentDomainId");
            }
        }

    }
}
