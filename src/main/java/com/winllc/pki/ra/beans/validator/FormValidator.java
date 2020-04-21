package com.winllc.pki.ra.beans.validator;

public interface FormValidator<T> {

    ValidationResponse validate(T form, boolean editMode);
}
