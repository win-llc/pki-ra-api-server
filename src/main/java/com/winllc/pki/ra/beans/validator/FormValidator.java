package com.winllc.pki.ra.beans.validator;

public interface FormValidator<T> {

    boolean validate(T form, boolean editMode);
}
