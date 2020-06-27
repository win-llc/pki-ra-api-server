package com.winllc.pki.ra.service.external.vendorimpl;

import com.winllc.pki.ra.domain.ServerEntry;
import com.winllc.pki.ra.service.external.SecurityPolicyConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.util.HashMap;
import java.util.Map;

@Component
//todo should be loaded as a bean
public class OpenDJSecurityPolicyConnection implements SecurityPolicyConnection {

    //todo replace with dynamic
    @Value("${policy-server.ldap-url}")
    private String ldapUrl;
    @Value("${policy-server.ldap-username}")
    private String ldapUsername;
    @Value("${policy-server.ldap-password}")
    private String ldapPassword;
    @Value("${policy-server.ldap-base-dn}")
    private String ldapBaseDn;

    @Override
    public Map<String, String> getSecurityPolicyMapForService(String fqdn) {
        //todo
        Map<String, String> testMap = new HashMap<>();

        LdapTemplate ldapTemplate = buildLdapTemplate();
        
        return ldapTemplate.lookup("cn=" + fqdn, new AttributesMapper<Map<String, String>>() {
            @Override
            public Map<String, String> mapFromAttributes(Attributes attributes) throws NamingException {
                Map<String, String> map = new HashMap<>();
                Attribute secAttr = attributes.get("sn");
                String attrVal = (String) secAttr.get();
                map.put("testAttr1", attrVal);
                return map;
            }
        });
    }

    @Override
    public String getConnectionName() {
        return "opendj-security-policy-service";
    }


    private LdapTemplate buildLdapTemplate(){
        LdapContextSource contextSource = new LdapContextSource();

        contextSource.setUrl(ldapUrl);
        contextSource.setBase(ldapBaseDn);
        contextSource.setUserDn(ldapUsername);
        contextSource.setPassword(ldapPassword);
        contextSource.afterPropertiesSet();

        return new LdapTemplate(contextSource);
    }
}
