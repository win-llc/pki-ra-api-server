package com.winllc.pki.ra.service.external;

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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class EntityDirectoryService {
    //todo attach to a directory, allow attribute updating and validation

    private static final Logger log = LogManager.getLogger(EntityDirectoryService.class);

    @Autowired
    private AttributePolicyGroupRepository attributePolicyGroupRepository;
    @Autowired
    private SecurityPolicyService securityPolicyService;

    @Transactional
    public void applyServerEntryToDirectory(ServerEntry serverEntry){
        //todo

        //apply attributes from Attribute Policy Groups
        Set<AttributePolicyGroup> policyGroups = serverEntry.getAccount().getPolicyGroups();

        Map<String, Object> attributeMap = new HashMap<>();
        policyGroups.forEach(pg -> {
            Map<String, String> securityPolicyMap = new HashMap<>();
            if(StringUtils.isNotBlank(pg.getSecurityPolicyServiceName())) {
                securityPolicyMap.putAll(securityPolicyService.getSecurityPolicyMapForService(pg.getSecurityPolicyServiceName()));
            }

            pg.getAttributePolicies().forEach(ap -> {
                boolean addAttributeToMap = false;
                //if the AttributePolicy requires a security policy to apply the attribute, check the policy map
                //from the Policy Service
                if(ap.isValueFromSecurityPolicy()){
                    if(securityPolicyMap.containsKey(ap.getSecurityAttributeKeyName())){
                        addAttributeToMap = securityPolicyMap.get(ap.getSecurityAttributeKeyName())
                                .equals(ap.getSecurityAttributeValue());
                    }
                }else{
                    addAttributeToMap = true;
                }

                if(addAttributeToMap){
                    attributeMap.put(ap.getAttributeName(), ap.getAttributeValue());
                }
            });
        });

        applyAttributesToServerEntry(serverEntry, attributeMap);
    }

    private void applyAttributesToServerEntry(ServerEntry serverEntry, Map<String, Object> attributeValueMap){
        //todo add attributes to an entity service, such as an LDAP directory

        log.info("Will apply attributes to: "+serverEntry.getFqdn());
        for(Map.Entry<String, Object> entry: attributeValueMap.entrySet()){
            log.info("Attribute: "+entry.getKey() + " : " + entry.getValue());
        }

    }
}
