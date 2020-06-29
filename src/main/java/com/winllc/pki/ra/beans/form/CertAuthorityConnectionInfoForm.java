package com.winllc.pki.ra.beans.form;

import com.winllc.pki.ra.ca.CertAuthority;
import com.winllc.pki.ra.ca.ConnectionProperty;
import com.winllc.pki.ra.domain.CertAuthorityConnectionInfo;
import com.winllc.pki.ra.domain.CertAuthorityConnectionProperty;

import java.util.HashSet;
import java.util.Set;

public class CertAuthorityConnectionInfoForm extends ValidForm<CertAuthorityConnectionInfo> {

    private String name;
    private String type;
    private String trustChainBase64;
    private String baseUrl;
    private String issuePath;
    private String revokePath;
    private String searchPath;
    private Set<CertAuthorityConnectionProperty> properties;
    private String authKeyAlias;

    public CertAuthorityConnectionInfoForm() {
    }

    @Override
    protected void processIsValid() {

    }

    public CertAuthorityConnectionInfoForm(CertAuthorityConnectionInfo info, CertAuthority ca) {
        super(info);
        this.name = info.getName();
        this.type = info.getType().name();
        this.properties = info.getProperties();
        this.baseUrl = info.getBaseUrl();
        this.trustChainBase64 = info.getTrustChainBase64();
        this.authKeyAlias = info.getAuthKeyAlias();
        addRequiredPropertyPlaceholders(ca);
    }

    private void addRequiredPropertyPlaceholders(CertAuthority ca){
        if(properties == null) properties = new HashSet<>();
        for(ConnectionProperty connectionProperty : ca.getType().getRequiredProperties()){
            String requiredProp = connectionProperty.getName();
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

    public String getTrustChainBase64() {
        return trustChainBase64;
    }

    public void setTrustChainBase64(String trustChainBase64) {
        this.trustChainBase64 = trustChainBase64;
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
        if(properties == null) properties = new HashSet<>();
        return properties;
    }

    public void setProperties(Set<CertAuthorityConnectionProperty> properties) {
        this.properties = properties;
    }

    public String getAuthKeyAlias() {
        return authKeyAlias;
    }

    public void setAuthKeyAlias(String authKeyAlias) {
        this.authKeyAlias = authKeyAlias;
    }
}
