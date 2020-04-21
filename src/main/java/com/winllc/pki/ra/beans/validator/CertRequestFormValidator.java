package com.winllc.pki.ra.beans.validator;

import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.beans.form.CertificateRequestForm;
import org.springframework.stereotype.Component;

@Component
public class CertRequestFormValidator implements FormValidator<CertificateRequestForm> {
    @Override
    public ValidationResponse validate(CertificateRequestForm form, boolean editMode) {
        ValidationResponse validationResponse = new ValidationResponse();
        try {
            CertUtil.csrBase64ToPKC10Object(form.getCsr());
        } catch (Exception e) {
            validationResponse.addError("csr", e.getMessage());
        }

        return validationResponse;
    }
}
