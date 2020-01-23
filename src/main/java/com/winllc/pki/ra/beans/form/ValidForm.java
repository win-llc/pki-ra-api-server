package com.winllc.pki.ra.beans.form;

import com.winllc.pki.ra.beans.info.InfoObject;
import org.springframework.data.jpa.domain.AbstractPersistable;

public abstract class ValidForm<T extends AbstractPersistable<Long>> extends InfoObject<T> {

    protected ValidForm(T entity) {
        super(entity);
    }

    protected ValidForm() {
    }

    protected abstract boolean isValid();
}
