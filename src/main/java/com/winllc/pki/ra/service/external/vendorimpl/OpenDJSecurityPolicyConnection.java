package com.winllc.pki.ra.service.external.vendorimpl;

import com.winllc.pki.ra.domain.ServerEntry;
import com.winllc.pki.ra.service.external.SecurityPolicyConnection;
import com.winllc.pki.ra.service.external.SecurityPolicyServerProjectDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.stereotype.Component;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.time.LocalDate;
import java.util.*;

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

    private final String[] projectEntryAttributeLdapClass = {"top", "document"};

    @Override
    public Map<String, String> getSecurityPolicyMapForService(String fqdn, String projectId) {
        //todo
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
    public Optional<SecurityPolicyServerProjectDetails> getProjectDetails(String projectId) {
        //todo
        LdapTemplate ldapTemplate = buildLdapTemplate();

        SecurityPolicyServerProjectDetails details= ldapTemplate.lookup("cn=" + projectId+",ou=policy-server-projects", new ProjectDetailsMapper());

        if(details != null){
            return Optional.of(details);
        }else {
            return Optional.empty();
        }
    }

    @Override
    public List<SecurityPolicyServerProjectDetails> getAllProjects() {
        //todo

        LdapTemplate ldapTemplate = buildLdapTemplate();
        List<SecurityPolicyServerProjectDetails> search = ldapTemplate.search(LdapQueryBuilder.query().base("ou=policy-server-projects")
                .filter("objectclass=document"), new ProjectDetailsMapper());

        return search;
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

    private static class ProjectDetailsMapper implements AttributesMapper<SecurityPolicyServerProjectDetails> {

        @Override
        public SecurityPolicyServerProjectDetails mapFromAttributes(Attributes attributes) throws NamingException {
            SecurityPolicyServerProjectDetails details = new SecurityPolicyServerProjectDetails();
            Attribute descriptionAttribute = attributes.get("description");
            Object desc = descriptionAttribute.get();
            details.setProjectName(desc.toString());

            details.setProjectId(attributes.get("cn").get().toString());

            Map<String, Object> extraAttrs = new HashMap<>();

            NamingEnumeration<String> ids = attributes.getIDs();
            while(ids.hasMore()){
                String id = ids.next();
                Attribute attribute = attributes.get(id);
                String val = attribute.get().toString();
                switch (id){
                    case "documentLocation":
                        extraAttrs.put("AdminOrg", val);
                        break;
                    case "documentPublisher":
                        extraAttrs.put("DutyOrg", val);
                        break;
                    case "documentVersion":
                        extraAttrs.put("ATOStatus", val);
                        break;
                    case "documentTitle":
                        extraAttrs.put("LifeCycleStatus", val);
                        break;
                    case "organizationName":
                        extraAttrs.put("clearance", val);
                        break;
                }
            }

            details.setAllSecurityAttributesMap(extraAttrs);

            //description
            //documentLocation
            //documentPublisher
            //documentVersion
            //documentTitle
            //organizationName
            return details;
        }
    }
}
