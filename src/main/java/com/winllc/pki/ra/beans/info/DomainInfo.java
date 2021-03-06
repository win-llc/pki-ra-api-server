package com.winllc.pki.ra.beans.info;

import com.winllc.acme.common.domain.Domain;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DomainInfo extends InfoObject<Domain> {

    private String base;
    private String fullDomainName;
    private DomainInfo parentDomainInfo;
    private List<DomainInfo> subDomainInfo;

    public DomainInfo(Domain domain, boolean loadSubDomains){
        super(domain);
        this.base = domain.getBase();
        this.fullDomainName = domain.getFullDomainName();

        if(domain.getParentDomain() != null){
            DomainInfo parentInfo = new DomainInfo(domain.getParentDomain(), false);
            this.parentDomainInfo = parentInfo;
        }

        if(loadSubDomains) {
            if (!CollectionUtils.isEmpty(domain.getSubDomains())) {
                List<DomainInfo> infoList = new ArrayList<>();
                for (Domain subDomain : domain.getSubDomains()) {
                    infoList.add(new DomainInfo(subDomain, false));
                }
                subDomainInfo = infoList;
            }
        }
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getFullDomainName() {
        return fullDomainName;
    }

    public void setFullDomainName(String fullDomainName) {
        this.fullDomainName = fullDomainName;
    }

    public DomainInfo getParentDomainInfo() {
        return parentDomainInfo;
    }

    public void setParentDomainInfo(DomainInfo parentDomainInfo) {
        this.parentDomainInfo = parentDomainInfo;
    }

    public List<DomainInfo> getSubDomainInfo() {
        return subDomainInfo;
    }

    public void setSubDomainInfo(List<DomainInfo> subDomainInfo) {
        this.subDomainInfo = subDomainInfo;
    }
}
