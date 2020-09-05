package com.winllc.pki.ra.beans.form;

import com.winllc.acme.common.ca.CertAuthority;
import com.winllc.acme.common.ca.ConnectionProperty;
import com.winllc.acme.common.domain.CertAuthorityConnectionInfo;
import com.winllc.acme.common.domain.CertAuthorityConnectionProperty;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;

import java.util.HashSet;
import java.util.Set;

public class CertAuthorityConnectionInfoForm extends ValidForm<CertAuthorityConnectionInfo> {

    private String name;
    private String certAuthorityClassName;
    private String trustChainBase64;
    private String baseUrl;
    private Set<CertAuthorityConnectionProperty> properties;
    private String authKeyAlias;

    public CertAuthorityConnectionInfoForm() {
    }

    @Override
    protected void processIsValid() {
        UrlValidator urlValidator = new UrlValidator();

        if(StringUtils.isNotBlank(baseUrl) && !urlValidator.isValid(baseUrl)){
            getErrors().put("baseUrlInvalid", "Invalid base URL: "+baseUrl);
        }
    }

    public CertAuthorityConnectionInfoForm(CertAuthorityConnectionInfo info, CertAuthority ca) {
        super(info);
        this.name = info.getName();
        this.certAuthorityClassName = info.getCertAuthorityClassName();
        this.properties = info.getProperties();
        this.baseUrl = info.getBaseUrl();
        this.trustChainBase64 = info.getTrustChainBase64();
        this.authKeyAlias = info.getAuthKeyAlias();
        addRequiredPropertyPlaceholders(ca);
    }

    private void addRequiredPropertyPlaceholders(CertAuthority ca){
        if(properties == null) properties = new HashSet<>();
        for(ConnectionProperty connectionProperty : ca.getRequiredProperties()){
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

    public String getCertAuthorityClassName() {
        return certAuthorityClassName;
    }

    public void setCertAuthorityClassName(String certAuthorityClassName) {
        this.certAuthorityClassName = certAuthorityClassName;
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
