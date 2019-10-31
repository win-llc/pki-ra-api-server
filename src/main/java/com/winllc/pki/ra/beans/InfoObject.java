package com.winllc.pki.ra.beans;

import com.winllc.pki.ra.domain.BaseEntity;
import org.springframework.data.jpa.domain.AbstractPersistable;

public class InfoObject {

    private Long id;

    protected InfoObject(AbstractPersistable<Long> entity){
        this.id = entity.getId();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
