package com.winllc.pki.ra.beans;

import java.util.HashMap;
import java.util.Map;

public class AcmeClientDetails {
    /*
    ACME_EAB_HMAC_KEY=Mjc5ODA0NDIzOTU1MGExMDVlZjhiYTdmMTg3YzNjMGI2NTdkZGE1YTFhYTk1MDBkZDA5NTZkOTQzYmQ0ZTk0NTAxOTYxYmY4NGVjYzM1NzhjOTBhYTA5YjU1NzhiNWQzMTNhNzQ0ZTQ4ZmU3ZWNmNjBkMjBmMGFlNmQzZWJjNWU=
ACME_KID=kidtest
ACME_SERVER=http://192.168.1.13:8181/acme/directory
     */

    private static final String acmeEabHmacKeyName = "ACME_EAB_HMAC_KEY";
    private static final String acmeKidName = "ACME_KID";
    private static final String acmeServerName = "ACME_SERVER";

    private String acmeEabHmacKeyValue;
    private String acmeKidValue;
    private String acmeServerValue;

    public Map<String, String> buildMap(){
        Map<String, String> map = new HashMap<>();
        map.put(acmeEabHmacKeyName, acmeEabHmacKeyValue);
        map.put(acmeKidName, acmeKidValue);
        map.put(acmeServerName, acmeServerValue);
        return map;
    }

    public String getAcmeEabHmacKeyValue() {
        return acmeEabHmacKeyValue;
    }

    public void setAcmeEabHmacKeyValue(String acmeEabHmacKeyValue) {
        this.acmeEabHmacKeyValue = acmeEabHmacKeyValue;
    }

    public String getAcmeKidValue() {
        return acmeKidValue;
    }

    public void setAcmeKidValue(String acmeKidValue) {
        this.acmeKidValue = acmeKidValue;
    }

    public String getAcmeServerValue() {
        return acmeServerValue;
    }

    public void setAcmeServerValue(String acmeServerValue) {
        this.acmeServerValue = acmeServerValue;
    }
}
