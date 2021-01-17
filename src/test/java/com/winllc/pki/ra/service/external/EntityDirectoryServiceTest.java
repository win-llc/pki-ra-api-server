package com.winllc.pki.ra.service.external;

import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.*;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.AttributePolicyGroupRepository;
import com.winllc.pki.ra.repository.AttributePolicyRepository;
import com.winllc.pki.ra.service.AccountService;
import com.winllc.pki.ra.service.SecurityPolicyService;
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
import java.util.Optional;

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
    private AccountService accountService;
    @Autowired
    private AttributePolicyRepository attributePolicyRepository;
    @Autowired
    private AttributePolicyGroupRepository attributePolicyGroupRepository;

    @BeforeEach
    @Transactional
    void before(){
        Account account = Account.buildNew("Test Project 2");
        account.setKeyIdentifier("kidtest1");
        account.setSecurityPolicyServerProjectId("project1");
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
        Account account = accountRepository.findDistinctByProjectName("Test Project 2").get();

        ServerEntry serverEntry = ServerEntry.buildNew();
        serverEntry.setFqdn("test.winllc-dev.com");

        Map<String, Object> policyMapForServer = new HashMap<>();
        policyMapForServer.put("matchName", "matchValue");
        policyMapForServer.put("nameExists", "overrideValue");

        SecurityPolicyServerProjectDetails details = new SecurityPolicyServerProjectDetails();
        details.setProjectName("project1");
        details.setAllSecurityAttributesMap(policyMapForServer);
        when(securityPolicyService
        .getPolicyServerProjectDetails(any(), any()))
                .thenReturn(Optional.of(details));

        //will be added
        AttributePolicy apUseSecurityPolicyValue = new AttributePolicy();
        apUseSecurityPolicyValue.setUseSecurityAttributeValueIfNameExists(true);
        apUseSecurityPolicyValue.setSecurityAttributeKeyName("matchName");
        apUseSecurityPolicyValue.setSecurityAttributeValue("matchValue");
        apUseSecurityPolicyValue.setAttributeName("apUseSecurityPolicyValue");
        apUseSecurityPolicyValue.setAttributeValue("testValue");
        apUseSecurityPolicyValue = attributePolicyRepository.save(apUseSecurityPolicyValue);

        AttributePolicy apUseValueIfSecurityNameValueMatch = new AttributePolicy();
        apUseValueIfSecurityNameValueMatch.setUseValueIfSecurityAttributeNameValueExists(true);
        apUseValueIfSecurityNameValueMatch.setSecurityAttributeKeyName("matchName");
        apUseValueIfSecurityNameValueMatch.setSecurityAttributeValue("matchValue");
        apUseValueIfSecurityNameValueMatch.setAttributeName("apUseValueIfSecurityNameValueMatch");
        apUseValueIfSecurityNameValueMatch.setAttributeValue("testValue");
        apUseValueIfSecurityNameValueMatch = attributePolicyRepository.save(apUseValueIfSecurityNameValueMatch);

        AttributePolicy apNoMatchSecurityPolicyNameValue = new AttributePolicy();
        apNoMatchSecurityPolicyNameValue.setUseValueIfSecurityAttributeNameValueExists(true);
        apNoMatchSecurityPolicyNameValue.setSecurityAttributeKeyName("matchName");
        apNoMatchSecurityPolicyNameValue.setSecurityAttributeValue("noMatchValue");
        apNoMatchSecurityPolicyNameValue.setAttributeName("apNoMatchSecurityPolicyNameValue");
        apNoMatchSecurityPolicyNameValue.setAttributeValue("noAddValue");
        apNoMatchSecurityPolicyNameValue = attributePolicyRepository.save(apNoMatchSecurityPolicyNameValue);

        //will be added
        AttributePolicy apStaticValue = new AttributePolicy();
        apStaticValue.setStaticValue(true);
        apStaticValue.setAttributeName("apStaticValue");
        apStaticValue.setAttributeValue("testStaticValue");
        apStaticValue = attributePolicyRepository.save(apStaticValue);

        AttributePolicy apVariableValueFromServerEntry = new AttributePolicy();
        apVariableValueFromServerEntry.setAttributeName("apVariableValueFromServerEntry");
        apVariableValueFromServerEntry.setAttributeValue("{fqdn}");
        apVariableValueFromServerEntry = attributePolicyRepository.save(apVariableValueFromServerEntry);

        AttributePolicyGroup attributePolicyGroup = new AttributePolicyGroup();
        attributePolicyGroup.getAttributePolicies().add(apUseSecurityPolicyValue);
        attributePolicyGroup.getAttributePolicies().add(apUseValueIfSecurityNameValueMatch);
        attributePolicyGroup.getAttributePolicies().add(apNoMatchSecurityPolicyNameValue);
        attributePolicyGroup.getAttributePolicies().add(apStaticValue);
        attributePolicyGroup.getAttributePolicies().add(apVariableValueFromServerEntry);
        attributePolicyGroup.setAccount(account);
        attributePolicyGroup = attributePolicyGroupRepository.save(attributePolicyGroup);

        account.getPolicyGroups().add(attributePolicyGroup);
        account = accountRepository.save(account);

        serverEntry.setAccount(account);

        Map<String, Object> appliedAttributeMap = entityDirectoryService.calculateAttributePolicyMapForServerEntry(serverEntry);

        assertEquals("matchValue", appliedAttributeMap.get("apUseSecurityPolicyValue").toString());
        assertEquals("testValue", appliedAttributeMap.get("apUseValueIfSecurityNameValueMatch").toString());
        assertFalse(appliedAttributeMap.containsKey("apNoMatchSecurityPolicyNameValue"));
        assertEquals("testStaticValue", appliedAttributeMap.get("apStaticValue").toString());
        assertEquals(serverEntry.getFqdn(), appliedAttributeMap.get("apVariableValueFromServerEntry").toString());
    }
}