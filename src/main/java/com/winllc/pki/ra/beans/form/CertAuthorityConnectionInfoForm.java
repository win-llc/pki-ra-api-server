package com.winllc.pki.ra.beans.form;

import com.winllc.acme.common.ca.ConnectionProperty;
import com.winllc.acme.common.domain.CertAuthorityConnectionInfo;
import com.winllc.acme.common.domain.CertAuthorityConnectionProperty;
import com.winllc.ra.integration.ca.CertAuthority;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CertAuthorityConnectionInfoForm extends ValidForm<CertAuthorityConnectionInfo> {

    private String name;
    private String type;
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
        this.type = info.getCertAuthorityClassName();
        this.properties = info.getProperties();
        this.baseUrl = info.getBaseUrl();
        this.trustChainBase64 = info.getTrustChainBase64();
        this.authKeyAlias = info.getAuthKeyAlias();
        try {
            addRequiredPropertyPlaceholders(ca);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addRequiredPropertyPlaceholders(CertAuthority ca) throws Exception{
        if(properties == null) properties = new HashSet<>();

        //get required properties for the implemented CA type
        Method m = Class.forName(ca.getClass().getName()).getMethod("getRequiredProperties");
        Object result = m.invoke(null);

        if(result instanceof List){
            List<ConnectionProperty> properties = (List<ConnectionProperty>) result;
            for(ConnectionProperty connectionProperty : properties){
                String requiredProp = connectionProperty.getName();
                boolean containsProp = this.properties.stream()
                        .anyMatch(p -> p.getName().equals(requiredProp));
                if(!containsProp){
                    CertAuthorityConnectionProperty prop = new CertAuthorityConnectionProperty();
                    prop.setName(requiredProp);
                    prop.setValue("");
                    prop.setPassword(connectionProperty.getPassword());
                    this.properties.add(prop);
                }
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
