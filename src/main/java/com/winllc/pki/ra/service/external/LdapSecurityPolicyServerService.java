package com.winllc.pki.ra.service.external;

import com.winllc.pki.ra.constants.ServerSettingRequired;
import com.winllc.pki.ra.service.ServerSettingsService;
import com.winllc.pki.ra.service.external.vendorimpl.OpenDJSecurityPolicyConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.stereotype.Service;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.util.*;

@Service
public class LdapSecurityPolicyServerService implements SecurityPolicyConnection {

    @Autowired
    private ServerSettingsService serverSettingsService;

    @Override
    public String getConnectionName() {
        return null;
    }
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

        Optional<String> optionalSchemaType = serverSettingsService.getServerSettingValue(ServerSettingRequired.POLICY_SERVER_LDAP_SCHEMATYPE);
        String schemaType = optionalSchemaType.orElse("document");

        Optional<String> optionalProjectBaseDn = serverSettingsService.getServerSettingValue(ServerSettingRequired.POLICY_SERVER_LDAP_PROJECTSBASEDN);
        String projectBaseDn = optionalProjectBaseDn.orElse("ou=policy-server-projects");

        LdapTemplate ldapTemplate = buildLdapTemplate();
        List<SecurityPolicyServerProjectDetails> search = ldapTemplate.search(LdapQueryBuilder.query().base(projectBaseDn)
                .filter("objectclass="+schemaType), new ProjectDetailsMapper());

        return search;
    }

    private LdapTemplate buildLdapTemplate(){
        LdapContextSource contextSource = new LdapContextSource();
        Optional<String> optionalUrl = serverSettingsService.getServerSettingValue(ServerSettingRequired.POLICY_SERVER_LDAP_URL);
        Optional<String> optionalUsername = serverSettingsService.getServerSettingValue(ServerSettingRequired.POLICY_SERVER_LDAP_USERNAME);
        Optional<String> optionalPassword = serverSettingsService.getServerSettingValue(ServerSettingRequired.POLICY_SERVER_LDAP_PASSWORD);
        Optional<String> optionalBaseDn = serverSettingsService.getServerSettingValue(ServerSettingRequired.POLICY_SERVER_LDAP_BASEDN);

        optionalUrl.ifPresent(u -> contextSource.setUrl(u));
        optionalUsername.ifPresent(u -> contextSource.setUserDn(u));
        optionalPassword.ifPresent(u -> contextSource.setPassword(u));
        optionalBaseDn.ifPresent(u -> contextSource.setBase(u));

        contextSource.afterPropertiesSet();

        return new LdapTemplate(contextSource);
    }

    private static class ProjectDetailsMapper implements AttributesMapper<SecurityPolicyServerProjectDetails> {

        @Override
        public SecurityPolicyServerProjectDetails mapFromAttributes(Attributes attributes) throws NamingException {
            SecurityPolicyServerProjectDetails details = new SecurityPolicyServerProjectDetails();
            Attribute descriptionAttribute = attributes.get("cn");
            Object desc = descriptionAttribute.get();
            details.setProjectName(desc.toString());

            details.setProjectId(attributes.get("cn").get().toString());

            Map<String, List<String>> extraAttrs = new HashMap<>();

            NamingEnumeration<String> ids = attributes.getIDs();
            while(ids.hasMore()){
                String id = ids.next();
                Attribute attribute = attributes.get(id);
                Object val = attribute.get();

                List<String> temp = new ArrayList<>();
                if(attribute.size() > 1){
                    NamingEnumeration<?> all = attribute.getAll();
                    while(all.hasMore()){
                        Object next = all.next();
                        temp.add(next.toString());
                    }
                }else{
                    temp.add(val.toString());
                }

                extraAttrs.put(id, temp);

                /*
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

                 */
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
