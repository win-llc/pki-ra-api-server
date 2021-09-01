package com.winllc.pki.ra.service.external;

import com.winllc.pki.ra.constants.ServerSettingRequired;
import com.winllc.acme.common.domain.Account;
import com.winllc.acme.common.domain.AttributePolicyGroup;
import com.winllc.acme.common.domain.ServerEntry;
import com.winllc.acme.common.repository.AttributePolicyGroupRepository;
import com.winllc.acme.common.repository.ServerEntryRepository;
import com.winllc.pki.ra.service.SecurityPolicyService;
import com.winllc.pki.ra.service.ServerSettingsService;
import com.winllc.pki.ra.service.external.beans.DirectoryServerEntity;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.stereotype.Service;

import javax.naming.InvalidNameException;
import javax.transaction.Transactional;
import java.lang.reflect.Field;
import java.util.*;

import static com.winllc.pki.ra.constants.ServerSettingRequired.ENTITY_DIRECTORY_LDAP_SERVERBASEDN;

@Service
public class EntityDirectoryService {
    //todo attach to a directory, allow attribute updating and validation

    private static final Logger log = LogManager.getLogger(EntityDirectoryService.class);

    private final AttributePolicyGroupRepository attributePolicyGroupRepository;
    private final SecurityPolicyService securityPolicyService;
    private final ServerSettingsService serverSettingsService;
    private final ServerEntryRepository serverEntryRepository;

    public EntityDirectoryService(AttributePolicyGroupRepository attributePolicyGroupRepository, SecurityPolicyService securityPolicyService, ServerSettingsService serverSettingsService, ServerEntryRepository serverEntryRepository) {
        this.attributePolicyGroupRepository = attributePolicyGroupRepository;
        this.securityPolicyService = securityPolicyService;
        this.serverSettingsService = serverSettingsService;
        this.serverEntryRepository = serverEntryRepository;
    }

    //return the applied attribute map
    @Transactional
    public Map<String, Object> applyServerEntryToDirectory(ServerEntry serverEntry){

        //apply attributes from Attribute Policy Groups
        Map<String, Object> attributeMap = calculateAttributePolicyMapForServerEntry(serverEntry);

        try {
            applyAttributesToServerEntry(serverEntry, attributeMap);
        }catch (Exception e){
            log.error("Could not apply attributes", e);
        }

        return attributeMap;
    }

    public Map<String, Object> getCurrentAttributesForServer(ServerEntry serverEntry) throws InvalidNameException {
        String distinguishedName = serverEntry.getDistinguishedName();
        if(StringUtils.isNotBlank(distinguishedName)){
            LdapTemplate ldapTemplate = buildDirectoryLdapTemplate();
            DirectoryServerEntity entity = new DirectoryServerEntity(serverEntry, ldapTemplate);
            return entity.getCurrentAttributes();
        }else{
            log.debug("Could not find Server Entry in Directory");
            return new HashMap<>();
        }
    }

    private boolean applyAttributesToServerEntry(ServerEntry serverEntry, Map<String, Object> attributeValueMap){
        //todo add attributes to an entity service, such as an LDAP directory

        log.info("Will apply attributes to: "+serverEntry.getFqdn());
        for(Map.Entry<String, Object> entry: attributeValueMap.entrySet()){
            log.info("Attribute: "+entry.getKey() + " : " + entry.getValue());
        }

        Optional<String> serverBaseDnOptional = serverSettingsService.getServerSettingValue(ENTITY_DIRECTORY_LDAP_SERVERBASEDN);
        String baseDn = null;
        if(serverBaseDnOptional.isPresent()) {
            baseDn = serverBaseDnOptional.get();
        }

        serverEntry.buildDn(baseDn);
        serverEntry = serverEntryRepository.save(serverEntry);

        log.info("Going to add Server Entry to LDAP: "+serverEntry.getDistinguishedName());

        LdapTemplate ldapTemplate = buildDirectoryLdapTemplate();

        try {
            DirectoryServerEntity entity = new DirectoryServerEntity(serverEntry, ldapTemplate);
            entity.overwriteAttributes(attributeValueMap);
        } catch (InvalidNameException e) {
            log.error("Invalid name", e);
            return false;
        }

        return true;
    }

    public Map<String, Object> calculateAttributePolicyMapForServerEntry(ServerEntry serverEntry){
        Account account = serverEntry.getAccount();
        List<AttributePolicyGroup> policyGroups = attributePolicyGroupRepository.findAllByAccount(account);

        Map<String, Object> attributeMap = new HashMap<>();
        policyGroups.forEach(pg -> {
            Hibernate.initialize(pg.getAttributePolicies());
            Map<String, Object> securityPolicyMap = new HashMap<>();
            if(StringUtils.isNotBlank(account.getSecurityPolicyServerProjectId())) {
                try {
                    SecurityPolicyServerProjectDetails details = securityPolicyService
                            .getProjectDetails(account.getSecurityPolicyServerProjectId());

                        securityPolicyMap.putAll(details.getAllSecurityAttributesMap());

                    //Map<String, String> map = securityPolicyService
                    //        .getSecurityPolicyMapForService(null,
                    //                serverEntry.getFqdn(), account.getSecurityPolicyServerProjectId());
                    //securityPolicyMap.putAll(map);
                } catch (Exception e) {
                    log.error("Could not retrieve Security Policy", e);
                }
            }

            pg.getAttributePolicies().forEach(ap -> {
                String attributeValueToAdd = null;
                //if the AttributePolicy requires a security policy to apply the attribute, check the policy map
                //from the Policy Service

                if(ap.isPolicyServerValue()){
                    if(securityPolicyMap.containsKey(ap.getAttributeValue())) {
                        Object val = securityPolicyMap.get(ap.getAttributeValue());
                        attributeValueToAdd = val.toString();
                    }
                }else if(ap.isServerEntryValue()){
                    Optional<Object> optionalServerValue = getServerEntryField(serverEntry, ap.getVariableValueField());
                    if(optionalServerValue.isPresent()){
                        attributeValueToAdd = optionalServerValue.get().toString();
                    }
                }else if(ap.isStaticValue()){
                    attributeValueToAdd = ap.getAttributeValue();
                }

                if(attributeValueToAdd != null){
                    attributeMap.put(ap.getAttributeName(), attributeValueToAdd);
                }
            });
        });

        return attributeMap;
    }

    //if attribute policy value is in format {value} treat as variable
    private Optional<Object> getServerEntryField(ServerEntry serverEntry, String serverEntryField){
        try {
            Field field = serverEntry.getClass().getDeclaredField(serverEntryField);
            field.setAccessible(true);
            Object value = field.get(serverEntry);
            return Optional.of(value);
        }catch (Exception e){
            log.error("Could not find field: "+serverEntryField, e);
        }
        return Optional.empty();
    }

    //todo finish this
    private LdapTemplate buildDirectoryLdapTemplate(){
        LdapContextSource contextSource = new LdapContextSource();
        Optional<String> optionalUrl = serverSettingsService.getServerSettingValue(ServerSettingRequired.ENTITY_DIRECTORY_LDAP_URL);
        Optional<String> optionalUsername = serverSettingsService.getServerSettingValue(ServerSettingRequired.ENTITY_DIRECTORY_LDAP_USERNAME);
        Optional<String> optionalPassword = serverSettingsService.getServerSettingValue(ServerSettingRequired.ENTITY_DIRECTORY_LDAP_PASSWORD);

        optionalUrl.ifPresent(u -> contextSource.setUrl(u));
        optionalUsername.ifPresent(u -> contextSource.setUserDn(u));
        optionalPassword.ifPresent(u -> contextSource.setPassword(u));

        contextSource.afterPropertiesSet();

        return new LdapTemplate(contextSource);
    }
}
