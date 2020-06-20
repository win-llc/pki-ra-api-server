package com.winllc.pki.ra.service.external.vendorimpl;

import com.winllc.pki.ra.domain.ServerEntry;
import com.winllc.pki.ra.service.external.SecurityPolicyConnection;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
//todo should be loaded as a bean
public class OpenDJSecurityPolicyConnection implements SecurityPolicyConnection {

    private LdapTemplate ldapTemplate;

    @Override
    public Map<String, String> getSecurityPolicyMapForService(ServerEntry serverEntry) {
        //todo
        Map<String, String> testMap = new HashMap<>();

        //ldapTemplate.search("base",)

        return testMap;
    }

    @Override
    public String getConnectionName() {
        return "opendj";
    }

    public void setLdapTemplate(LdapTemplate ldapTemplate) {
        this.ldapTemplate = ldapTemplate;
    }
}
