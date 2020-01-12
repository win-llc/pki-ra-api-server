package com.winllc.pki.ra.beans;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ServerEntryDockerDeploymentFile {
    /*
    SERVER_NAME=test.winllc.com
OIDCRedirectURI=https://test.winllc.com/test
OIDCProviderMetadataURL=https://192.168.1.124:8443/auth/realms/dev/.well-known/openid-configuration
OIDCClientID=httpd-test-client
OIDCClientSecret=b61ef996-740e-4655-bf14-61f60bedba87
ProxyAddress=http://192.168.1.13:8282/test
ACME_EAB_HMAC_KEY=Mjc5ODA0NDIzOTU1MGExMDVlZjhiYTdmMTg3YzNjMGI2NTdkZGE1YTFhYTk1MDBkZDA5NTZkOTQzYmQ0ZTk0NTAxOTYxYmY4NGVjYzM1NzhjOTBhYTA5YjU1NzhiNWQzMTNhNzQ0ZTQ4ZmU3ZWNmNjBkMjBmMGFlNmQzZWJjNWU=
ACME_KID=kidtest
ACME_SERVER=http://192.168.1.13:8181/acme/directory
     */

    private static final String serverNameName = "SERVER_NAME";
    private static final String proxyAddressName = "ProxyAddress";

    private OIDCClientDetails oidcClientDetails;
    private AcmeClientDetails acmeClientDetails;
    private String serverNameValue;
    private String proxyAddressValue;

    public List<String> buildContent(){
        List<String> content = new LinkedList<>();
        for(Map.Entry<String, String> entry : oidcClientDetails.buildMap().entrySet()){
            content.add(entry.getKey()+"="+entry.getValue());
        }
        for(Map.Entry<String, String> entry : acmeClientDetails.buildMap().entrySet()){
            content.add(entry.getKey()+"="+entry.getValue());
        }
        content.add(serverNameName+"="+serverNameValue);
        content.add(proxyAddressName+"="+proxyAddressValue);
        return content;
    }

    public OIDCClientDetails getOidcClientDetails() {
        return oidcClientDetails;
    }

    public void setOidcClientDetails(OIDCClientDetails oidcClientDetails) {
        this.oidcClientDetails = oidcClientDetails;
    }

    public AcmeClientDetails getAcmeClientDetails() {
        return acmeClientDetails;
    }

    public void setAcmeClientDetails(AcmeClientDetails acmeClientDetails) {
        this.acmeClientDetails = acmeClientDetails;
    }

    public String getServerNameValue() {
        return serverNameValue;
    }

    public void setServerNameValue(String serverNameValue) {
        this.serverNameValue = serverNameValue;
    }

    public String getProxyAddressValue() {
        return proxyAddressValue;
    }

    public void setProxyAddressValue(String proxyAddressValue) {
        this.proxyAddressValue = proxyAddressValue;
    }
}
