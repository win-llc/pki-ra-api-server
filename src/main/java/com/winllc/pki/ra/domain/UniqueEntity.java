package com.winllc.pki.ra.domain;

import org.hibernate.annotations.Type;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.MappedSuperclass;
import java.util.UUID;

@MappedSuperclass
public abstract class UniqueEntity extends AbstractPersistable<Long> {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
