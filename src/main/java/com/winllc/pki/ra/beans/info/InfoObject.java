package com.winllc.pki.ra.beans.info;

import org.springframework.data.jpa.domain.AbstractPersistable;

public abstract class InfoObject<T extends AbstractPersistable<Long>> {

    private Long id;

    protected InfoObject(T entity){
        this.id = entity.getId();
    }

    protected InfoObject(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
