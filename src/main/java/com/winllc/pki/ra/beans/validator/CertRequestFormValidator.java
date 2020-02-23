package com.winllc.pki.ra.beans.validator;

import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.beans.form.CertificateRequestForm;
import org.springframework.stereotype.Component;

@Component
public class CertRequestFormValidator implements FormValidator<CertificateRequestForm> {
    @Override
    public boolean validate(CertificateRequestForm form, boolean editMode) {
        boolean valid = false;
        try {
            CertUtil.csrBase64ToPKC10Object(form.getCsr());
            valid = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return valid;
    }
}
