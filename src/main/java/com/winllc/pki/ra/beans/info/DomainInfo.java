package com.winllc.pki.ra.beans.info;

import com.winllc.pki.ra.beans.info.InfoObject;
import com.winllc.pki.ra.domain.Domain;

public class DomainInfo extends InfoObject<Domain> {

    private String base;

    public DomainInfo(Domain domain){
        super(domain);
        this.base = domain.getBase();
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }
}
