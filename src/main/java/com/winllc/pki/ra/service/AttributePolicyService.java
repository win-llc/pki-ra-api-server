package com.winllc.pki.ra.service;

import com.winllc.pki.ra.domain.AttributePolicyGroup;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.AttributePolicyGroupRepository;
import com.winllc.pki.ra.repository.AttributePolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/attributePolicy")
public class AttributePolicyService {

    @Autowired
    private AttributePolicyGroupRepository attributePolicyGroupRepository;
    @Autowired
    private AttributePolicyRepository attributePolicyRepository;

    @RequestMapping("/group/byId/{id}")
    public ResponseEntity<?> findPolicyGroupById(@PathVariable Long id) throws RAObjectNotFoundException {
        Optional<AttributePolicyGroup> policyGroupOptional = attributePolicyGroupRepository.findById(id);
        if(policyGroupOptional.isPresent()){
            return ResponseEntity.ok(policyGroupOptional.get());
        }else{
            throw new RAObjectNotFoundException(AttributePolicyGroup.class, id);
        }
    }

    public ResponseEntity<?> createGroupPolicyGroup(){
        //todo
        return null;
    }

}
