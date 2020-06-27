package com.winllc.pki.ra.service.external;

import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.AttributePolicyGroup;
import com.winllc.pki.ra.domain.ServerEntry;
import com.winllc.pki.ra.repository.AttributePolicyGroupRepository;
import com.winllc.pki.ra.service.ServerEntryService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.lang.reflect.Field;
import java.util.*;

@Service
public class EntityDirectoryService {
    //todo attach to a directory, allow attribute updating and validation

    private static final Logger log = LogManager.getLogger(EntityDirectoryService.class);

    @Autowired
    private AttributePolicyGroupRepository attributePolicyGroupRepository;
    @Autowired
    private SecurityPolicyService securityPolicyService;

    //return the applied attribute map
    @Transactional
    public Map<String, Object> applyServerEntryToDirectory(ServerEntry serverEntry){
        //todo

        //apply attributes from Attribute Policy Groups
        Map<String, Object> attributeMap = calculateAttributeMapForServerEntry(serverEntry);

        applyAttributesToServerEntry(serverEntry, attributeMap);

        return attributeMap;
    }

    private boolean applyAttributesToServerEntry(ServerEntry serverEntry, Map<String, Object> attributeValueMap){
        //todo add attributes to an entity service, such as an LDAP directory

        log.info("Will apply attributes to: "+serverEntry.getFqdn());
        for(Map.Entry<String, Object> entry: attributeValueMap.entrySet()){
            log.info("Attribute: "+entry.getKey() + " : " + entry.getValue());
        }

        return true;
    }

    public Map<String, Object> calculateAttributeMapForServerEntry(ServerEntry serverEntry){
        Account account = serverEntry.getAccount();
        List<AttributePolicyGroup> policyGroups = attributePolicyGroupRepository.findAllByAccount(account);

        Map<String, Object> attributeMap = new HashMap<>();
        policyGroups.forEach(pg -> {
            Map<String, String> securityPolicyMap = new HashMap<>();
            if(StringUtils.isNotBlank(pg.getSecurityPolicyServiceName())) {
                try {
                    Map<String, String> map = securityPolicyService
                            .getSecurityPolicyMapForService(pg.getSecurityPolicyServiceName(), serverEntry.getFqdn());
                    securityPolicyMap.putAll(map);
                } catch (Exception e) {
                    log.error("Could not retrieve Security Policy", e);
                }
            }

            pg.getAttributePolicies().forEach(ap -> {
                String attributeValueToAdd = null;
                //if the AttributePolicy requires a security policy to apply the attribute, check the policy map
                //from the Policy Service
                if(ap.checkSecurityPolicyBackedAttribute()){
                    //todo attribute value should be able to come from the security policy,
                    //not just allow value if key/value pair is found
                    if(securityPolicyMap.containsKey(ap.getSecurityAttributeKeyName())){
                        if(ap.isUseSecurityAttributeValueIfNameExists()){
                            attributeValueToAdd = securityPolicyMap.get(ap.getSecurityAttributeKeyName());
                        }else if(ap.isUseValueIfSecurityAttributeNameValueExists()){
                            String securityPolicy = securityPolicyMap.get(ap.getSecurityAttributeKeyName());
                            if(securityPolicy.equalsIgnoreCase(ap.getSecurityAttributeValue())){
                                attributeValueToAdd = ap.getAttributeValue();
                            }
                        }
                    }
                }else{
                    if(ap.isStaticValue()){
                        attributeValueToAdd = ap.getAttributeValue();
                    }else{
                        Optional<Object> optionalServerValue = getServerEntryField(serverEntry, ap.getVariableValueField());
                        if(optionalServerValue.isPresent()){
                            attributeValueToAdd = optionalServerValue.get().toString();
                        }
                    }
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
}
