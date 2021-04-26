package com.winllc.pki.ra.domain;

import com.winllc.acme.common.domain.BaseEntity;

import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class UniqueEntity extends BaseEntity {

    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

}
