package com.winllc.pki.ra.service.external;

import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.*;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.AttributePolicyGroupRepository;
import com.winllc.pki.ra.repository.AttributePolicyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class EntityDirectoryServiceTest {

    @Autowired
    private EntityDirectoryService entityDirectoryService;
    @MockBean
    private SecurityPolicyService securityPolicyService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AttributePolicyRepository attributePolicyRepository;
    @Autowired
    private AttributePolicyGroupRepository attributePolicyGroupRepository;

    @BeforeEach
    @Transactional
    void before(){
        Account account = new Account();
        account.setKeyIdentifier("kidtest1");
        account.setProjectName("Test Project 2");
        accountRepository.save(account);
    }

    @AfterEach
    @Transactional
    void after(){
        accountRepository.deleteAll();
    }

    @Test
    @Transactional
    void applyServerEntryToDirectory() throws Exception {
        Account account = accountRepository.findByKeyIdentifierEquals("kidtest1").get();

        ServerEntry serverEntry = new ServerEntry();
        serverEntry.setFqdn("test.winllc-dev.com");

        Map<String, String> policyMapForServer = new HashMap<>();
        policyMapForServer.put("matchName", "matchValue");

        when(securityPolicyService
                .getSecurityPolicyMapForService("secpolicysvc", serverEntry)).thenReturn(policyMapForServer);

        //will be added
        AttributePolicy attributePolicy = new AttributePolicy();
        attributePolicy.setValueFromSecurityPolicy(true);
        attributePolicy.setSecurityAttributeKeyName("matchName");
        attributePolicy.setSecurityAttributeValue("matchValue");
        attributePolicy.setAttributeName("testName");
        attributePolicy.setAttributeValue("testValue");
        attributePolicy = attributePolicyRepository.save(attributePolicy);

        //will not be added
        AttributePolicy attributePolicy2 = new AttributePolicy();
        attributePolicy2.setValueFromSecurityPolicy(true);
        attributePolicy2.setSecurityAttributeKeyName("noMatchName");
        attributePolicy2.setSecurityAttributeValue("noMatchValue");
        attributePolicy2.setAttributeName("testName2");
        attributePolicy2.setAttributeValue("testValue2");
        attributePolicy2 = attributePolicyRepository.save(attributePolicy2);

        //will be added
        AttributePolicy attributePolicy3 = new AttributePolicy();
        attributePolicy3.setValueFromSecurityPolicy(false);
        attributePolicy3.setAttributeName("testStaticName");
        attributePolicy3.setAttributeValue("testStaticValue");
        attributePolicy3 = attributePolicyRepository.save(attributePolicy3);

        AttributePolicyGroup attributePolicyGroup = new AttributePolicyGroup();
        attributePolicyGroup.setSecurityPolicyServiceName("secpolicysvc");
        attributePolicyGroup.getAttributePolicies().add(attributePolicy);
        attributePolicyGroup.getAttributePolicies().add(attributePolicy2);
        attributePolicyGroup.getAttributePolicies().add(attributePolicy3);
        attributePolicyGroup.setAccount(account);
        attributePolicyGroup = attributePolicyGroupRepository.save(attributePolicyGroup);

        account.getPolicyGroups().add(attributePolicyGroup);
        account = accountRepository.save(account);

        serverEntry.setAccount(account);

        Map<String, Object> appliedAttributeMap = entityDirectoryService.applyServerEntryToDirectory(serverEntry);
        assertEquals(2, appliedAttributeMap.size());
    }
}