package com.winllc.pki.ra.beans.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.winllc.pki.ra.beans.info.InfoObject;
import com.winllc.acme.common.domain.AccountOwnedEntity;
import com.winllc.acme.common.domain.BaseEntity;

import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;

public abstract class ValidForm<T extends BaseEntity> extends InfoObject<T>  {

    protected Map<String, String> errors = new HashMap<>();


    protected ValidForm(T entity) {
        super(entity);
    }

    protected ValidForm() {
        try {
            setClazz((Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]);
        }catch (ClassCastException e){ }
    }

    @JsonIgnore
    public boolean isAccountLinkedForm(){
        return AccountOwnedEntity.class.isAssignableFrom(getFormObjectType());
    }


    protected abstract void processIsValid();

    @JsonIgnore
    public boolean isValid(){
        processIsValid();
        return errors.size() == 0;
    }



    public Map<String, String> getErrors() {
        return errors;
    }
}
