package com.winllc.pki.ra.beans.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.winllc.pki.ra.ca.CertAuthority;
import com.winllc.pki.ra.ca.CertAuthorityConnectionType;
import com.winllc.pki.ra.domain.CertAuthorityConnectionInfo;
import com.winllc.pki.ra.domain.CertAuthorityConnectionProperty;

import javax.persistence.Column;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CertAuthorityConnectionInfoForm extends ValidForm<CertAuthorityConnectionInfo> {

    private String name;
    private String type;
    private String baseUrl;
    private String issuePath;
    private String revokePath;
    private String searchPath;
    private Set<CertAuthorityConnectionProperty> properties;

    private CertAuthorityConnectionInfoForm() {
    }

    @Override
    protected void processIsValid() {

    }

    public CertAuthorityConnectionInfoForm(CertAuthorityConnectionInfo info, CertAuthority ca) {
        super(info);
        this.name = info.getName();
        this.type = info.getType().name();
        this.properties = info.getProperties();
        addRequiredPropertyPlaceholders(ca);
    }

    private void addRequiredPropertyPlaceholders(CertAuthority ca){
        if(properties == null) properties = new HashSet<>();
        for(String requiredProp : ca.getRequiredConnectionProperties()){
            boolean containsProp = this.properties.stream()
                    .anyMatch(p -> p.getName().equals(requiredProp));
            if(!containsProp){
                CertAuthorityConnectionProperty prop = new CertAuthorityConnectionProperty();
                prop.setName(requiredProp);
                prop.setValue("");
                this.properties.add(prop);
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getIssuePath() {
        return issuePath;
    }

    public void setIssuePath(String issuePath) {
        this.issuePath = issuePath;
    }

    public String getRevokePath() {
        return revokePath;
    }

    public void setRevokePath(String revokePath) {
        this.revokePath = revokePath;
    }

    public String getSearchPath() {
        return searchPath;
    }

    public void setSearchPath(String searchPath) {
        this.searchPath = searchPath;
    }

    public Set<CertAuthorityConnectionProperty> getProperties() {
        return properties;
    }

    public void setProperties(Set<CertAuthorityConnectionProperty> properties) {
        this.properties = properties;
    }
}
