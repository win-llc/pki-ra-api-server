package com.winllc.pki.ra.beans;

import java.util.HashMap;
import java.util.Map;

public class OIDCClientDetails {
    /*
    OIDCRedirectURI=https://test.winllc.com/test
OIDCProviderMetadataURL=https://192.168.1.124:8443/auth/realms/dev/.well-known/openid-configuration
OIDCClientID=httpd-test-client
OIDCClientSecret=b61ef996-740e-4655-bf14-61f60bedba87
     */

    private static final String oidcRedirectUriName = "OIDCRedirectURI";
    private static final String oidcProviderMetadataUrlName = "OIDCProviderMetadataURL";
    private static final String oidcClientIdName = "OIDCClientID";
    private static final String oidcSecretName = "OIDCClientSecret";

    private String oidcRedirectUriValue;
    private String oidcProviderMetadataUrlValue;
    private String oidcClientIdValue;
    private String oidcSecretValue;

    public Map<String, String> buildMap(){
        Map<String, String> map = new HashMap<>();
        map.put(oidcRedirectUriName, oidcRedirectUriValue);
        map.put(oidcProviderMetadataUrlName, oidcProviderMetadataUrlValue);
        map.put(oidcClientIdName, oidcClientIdValue);
        map.put(oidcSecretName, oidcSecretValue);
        return map;
    }

    public String getOidcRedirectUriValue() {
        return oidcRedirectUriValue;
    }

    public void setOidcRedirectUriValue(String oidcRedirectUriValue) {
        this.oidcRedirectUriValue = oidcRedirectUriValue;
    }

    public String getOidcProviderMetadataUrlValue() {
        return oidcProviderMetadataUrlValue;
    }

    public void setOidcProviderMetadataUrlValue(String oidcProviderMetadataUrlValue) {
        this.oidcProviderMetadataUrlValue = oidcProviderMetadataUrlValue;
    }

    public String getOidcClientIdValue() {
        return oidcClientIdValue;
    }

    public void setOidcClientIdValue(String oidcClientIdValue) {
        this.oidcClientIdValue = oidcClientIdValue;
    }

    public String getOidcSecretValue() {
        return oidcSecretValue;
    }

    public void setOidcSecretValue(String oidcSecretValue) {
        this.oidcSecretValue = oidcSecretValue;
    }
}
