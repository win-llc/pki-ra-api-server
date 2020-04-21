package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.form.AttributePolicyGroupForm;
import com.winllc.pki.ra.domain.AttributePolicy;
import com.winllc.pki.ra.domain.AttributePolicyGroup;
import com.winllc.pki.ra.domain.ServerEntry;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.AttributePolicyGroupRepository;
import com.winllc.pki.ra.repository.AttributePolicyRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/attributePolicy")
public class AttributePolicyService {

    private static final Logger log = LogManager.getLogger(AttributePolicyService.class);

    @Autowired
    private AttributePolicyGroupRepository attributePolicyGroupRepository;
    @Autowired
    private AttributePolicyRepository attributePolicyRepository;

    @GetMapping("/group/byId/{id}")
    public ResponseEntity<?> findPolicyGroupById(@PathVariable Long id) throws RAObjectNotFoundException {
        Optional<AttributePolicyGroup> policyGroupOptional = attributePolicyGroupRepository.findById(id);
        if(policyGroupOptional.isPresent()){
            return ResponseEntity.ok(policyGroupOptional.get());
        }else{
            throw new RAObjectNotFoundException(AttributePolicyGroup.class, id);
        }
    }

    @PostMapping("/group/create")
    public ResponseEntity<?> createGroupPolicyGroup(@Valid @RequestBody AttributePolicyGroupForm form){
        AttributePolicyGroup attributePolicyGroup = new AttributePolicyGroup();
        //attributePolicyGroup.setAttributePolicies();

        //todo
        return null;
    }

    @PostMapping("/group/update")
    public ResponseEntity<?> updateGroupPolicyGroup(@Valid @RequestBody AttributePolicyGroupForm form){
        //todo

        return null;
    }



    private void updateServerEntriesWithPolicyGroup(AttributePolicyGroup attributePolicyGroup){

        Set<ServerEntry> serverEntrySet = attributePolicyGroup.getServerEntries();

        for(ServerEntry serverEntry : serverEntrySet) {
            Map<String, Object> attributeUpdateMap = new HashMap<>();
            for (AttributePolicy attributePolicy : attributePolicyGroup.getAttributePolicies()) {
                String attributeValue = attributePolicy.getAttributeValue();
                if(attributePolicy.isVariableValue()){
                    Optional<Object> optionalFieldValue = getServerEntryField(serverEntry, attributePolicy.getVariableValueField());
                    if(optionalFieldValue.isPresent()){
                        attributeValue = optionalFieldValue.get().toString();
                    }else{
                        log.error("Could not get ServerEntry field: "+attributePolicy.getVariableValueField());
                        attributeValue = "";
                    }
                }
                attributeUpdateMap.put(attributePolicy.getAttributeName(), attributeValue);
            }
        }
    }

    //if attribute policy value is in format {value} treat as variable
    private Optional<Object> getServerEntryField(ServerEntry serverEntry, String serverEntryField){
        Class  aClass = ServerEntry.class;
        Field field = null;
        try {
            field = aClass.getField(serverEntryField);

            String objectInstance = new String();

            Object value = field.get(objectInstance);
            return Optional.of(value);
        } catch (Exception e) {
            log.error("Could not find field on ServerEntry: "+serverEntryField, e);
        }
        return Optional.empty();
    }


}
