package com.winllc.pki.ra.beans.form;

import com.winllc.pki.ra.beans.info.InfoObject;
import com.winllc.pki.ra.domain.AccountOwnedEntity;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public abstract class ValidForm<T extends AbstractPersistable<Long>> extends InfoObject<T>  {

    private Class clazz;
    protected Map<String, String> errors = new HashMap<>();

    protected ValidForm(T entity) {
        super(entity);
        clazz = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    protected ValidForm() {
        clazz = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    public boolean isAccountLinkedForm(){
        return AccountOwnedEntity.class.isAssignableFrom(getFormObjectType());
    }

    public Class getFormObjectType(){
        return clazz;
    }

    protected abstract void processIsValid();

    public boolean isValid(){
        processIsValid();
        return errors.size() == 0;
    }


    public Map<String, String> getErrors() {
        return errors;
    }
}
