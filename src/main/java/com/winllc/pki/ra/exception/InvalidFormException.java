package com.winllc.pki.ra.exception;

import com.winllc.pki.ra.beans.form.ValidForm;

public class InvalidFormException extends RAException {
    public InvalidFormException(ValidForm validForm) {
        super("Invalid form: "+validForm.toString());
    }
}
