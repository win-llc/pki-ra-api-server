package com.winllc.pki.ra.service.external;

import com.winllc.acme.common.domain.Account;
import com.winllc.acme.common.repository.*;
import com.winllc.pki.ra.BaseTest;
import com.winllc.acme.common.domain.*;
import com.winllc.pki.ra.service.SecurityPolicyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.transaction.Transactional;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class EntityDirectoryServiceTest extends BaseTest {

    @Autowired
    private EntityDirectoryService entityDirectoryService;
    @MockBean
    private SecurityPolicyService securityPolicyService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private ServerEntryRepository serverEntryRepository;
    @Autowired
    private AttributePolicyRepository attributePolicyRepository;
    @Autowired
    private AttributePolicyGroupRepository attributePolicyGroupRepository;
    @Autowired
    private LdapSchemaOverlayRepository ldapSchemaOverlayRepository;

    @BeforeEach
    @Transactional
    void before(){
        Account account = Account.buildNew("Test Project 2");
        account.setKeyIdentifier("kidtest1");
        account.setSecurityPolicyServerProjectId("project1");
        accountRepository.save(account);

        LdapSchemaOverlayAttribute overlayAttribute = new LdapSchemaOverlayAttribute();
        overlayAttribute.setName("attr");
        overlayAttribute.setEnabled(true);

        LdapSchemaOverlay overlay = new LdapSchemaOverlay();
        overlay.setLdapObjectType("test");
        overlay.getAttributeMap().add(overlayAttribute);
        ldapSchemaOverlayRepository.save(overlay);
    }

    @AfterEach
    @Transactional
    void after(){
        accountRepository.deleteAll();
        ldapSchemaOverlayRepository.deleteAll();
    }

    @Test
    @Transactional
    void applyServerEntryToDirectory() throws Exception {
        Account account = accountRepository.findDistinctByProjectName("Test Project 2").get();

        ServerEntry serverEntry = ServerEntry.buildNew();
        serverEntry.setFqdn("test.winllc-dev.com");

        Map<String, List<String>> policyMapForServer = new HashMap<>();
        policyMapForServer.put("matchName", Collections.singletonList("matchValue"));
        policyMapForServer.put("nameExists", Collections.singletonList("overrideValue"));

        SecurityPolicyServerProjectDetails details = new SecurityPolicyServerProjectDetails();
        details.setProjectName("project1");
        details.setAllSecurityAttributesMap(policyMapForServer);
        when(securityPolicyService
        .getProjectDetails(any()))
                .thenReturn(details);

        //will be added
        AttributePolicy apUseSecurityPolicyValue = new AttributePolicy();
        apUseSecurityPolicyValue.setPolicyServerValue(true);
        apUseSecurityPolicyValue.setAttributeName("apUseSecurityPolicyValue");
        apUseSecurityPolicyValue.setAttributeValue("testValue");
        apUseSecurityPolicyValue = attributePolicyRepository.save(apUseSecurityPolicyValue);

        AttributePolicy apUseValueIfSecurityNameValueMatch = new AttributePolicy();
        apUseValueIfSecurityNameValueMatch.setPolicyServerValue(true);
        apUseValueIfSecurityNameValueMatch.setAttributeName("apUseValueIfSecurityNameValueMatch");
        apUseValueIfSecurityNameValueMatch.setAttributeValue("testValue");
        apUseValueIfSecurityNameValueMatch = attributePolicyRepository.save(apUseValueIfSecurityNameValueMatch);

        AttributePolicy apNoMatchSecurityPolicyNameValue = new AttributePolicy();
        apNoMatchSecurityPolicyNameValue.setPolicyServerValue(true);
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
        apVariableValueFromServerEntry.setServerEntryValue(true);
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
        serverEntry = serverEntryRepository.save(serverEntry);

        Map<String, Object> appliedAttributeMap = entityDirectoryService.calculateAttributePolicyMapForServerEntry(serverEntry);

        //todo re-add this
        //assertEquals("matchValue", appliedAttributeMap.get("apUseSecurityPolicyValue").toString());
        //assertEquals("testValue", appliedAttributeMap.get("apUseValueIfSecurityNameValueMatch").toString());
        assertFalse(appliedAttributeMap.containsKey("apNoMatchSecurityPolicyNameValue"));
        assertEquals("testStaticValue", appliedAttributeMap.get("apStaticValue").toString());
        assertEquals(serverEntry.getFqdn(), appliedAttributeMap.get("apVariableValueFromServerEntry").toString());
    }
}