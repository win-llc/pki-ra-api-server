package com.winllc.pki.ra.service.validators;

import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.beans.form.CertificateRequestDecisionForm;
import com.winllc.acme.common.domain.CertificateRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class CertificateRequestDecisionValidator implements Validator {

    private static final Logger log = LogManager.getLogger(CertificateRequestDecisionValidator.class);

    @Override
    public boolean supports(Class<?> clazz) {
        return CertificateRequestDecisionForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {

        CertificateRequestDecisionForm form = (CertificateRequestDecisionForm) target;


        String status = form.getStatus();
        if(StringUtils.isNotBlank(status)) {
            if (!status.equalsIgnoreCase("approved") && !status.equalsIgnoreCase("rejected")) {
                errors.rejectValue("status", "certificateRequestDecision.invalidStatus");
            }
        }else{
            errors.rejectValue("status", "certificateRequestDecision.emptyStatus");
        }

    }
}
