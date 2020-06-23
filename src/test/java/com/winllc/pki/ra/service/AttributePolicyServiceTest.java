package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.form.AttributePolicyGroupForm;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.AttributePolicy;
import com.winllc.pki.ra.domain.AttributePolicyGroup;
import com.winllc.pki.ra.domain.PocEntry;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.AttributePolicyGroupRepository;
import com.winllc.pki.ra.repository.AttributePolicyRepository;
import com.winllc.pki.ra.repository.PocEntryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class AttributePolicyServiceTest {

    @Autowired
    private AttributePolicyService attributePolicyService;
    @Autowired
    private AttributePolicyGroupRepository attributePolicyGroupRepository;
    @Autowired
    private AttributePolicyRepository attributePolicyRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PocEntryRepository pocEntryRepository;

    @BeforeEach
    void beforeEach(){
        Account account = new Account();
        account.setKeyIdentifier("kidtest1");
        account.setMacKey("testmac1");
        account.setProjectName("Test Project");
        account = accountRepository.save(account);

        PocEntry pocEntry = new PocEntry();
        pocEntry.setEmail("test@test.com");
        pocEntry.setAccount(account);
        pocEntry = pocEntryRepository.save(pocEntry);

        AttributePolicyGroup attributePolicyGroup = new AttributePolicyGroup();
        attributePolicyGroup.setAccount(account);
        attributePolicyGroup.setName("Test Group");
        attributePolicyGroup = attributePolicyGroupRepository.save(attributePolicyGroup);

        AttributePolicy staticAttribute = new AttributePolicy();
        staticAttribute.setAttributeName("staticAttr");
        staticAttribute.setStaticValue(true);
        staticAttribute.setAttributeValue("staticVal");
        staticAttribute = attributePolicyRepository.save(staticAttribute);

        AttributePolicy dynamicAttribute = new AttributePolicy();
        dynamicAttribute.setAttributeName("dynamicAttr");
        dynamicAttribute.setStaticValue(false);
        dynamicAttribute.setAttributeValue("{fqdn}");
        dynamicAttribute = attributePolicyRepository.save(dynamicAttribute);

        attributePolicyGroup.getAttributePolicies().add(staticAttribute);
        attributePolicyGroup.getAttributePolicies().add(dynamicAttribute);

        attributePolicyGroupRepository.save(attributePolicyGroup);
    }

    @AfterEach
    void afterEach(){
        attributePolicyGroupRepository.deleteAll();
        accountRepository.deleteAll();
        pocEntryRepository.deleteAll();
    }

    @Test
    void findPolicyGroupById() throws RAObjectNotFoundException {
        Optional<Account> optionalAccount = accountRepository.findByKeyIdentifierEquals("kidtest1");

        AttributePolicyGroup testGroup = new AttributePolicyGroup();
        testGroup.setName("test");
        testGroup.setAccount(optionalAccount.get());
        testGroup = attributePolicyGroupRepository.save(testGroup);

        AttributePolicyGroupForm policyGroupById = attributePolicyService.findPolicyGroupById(testGroup.getId());
        assertEquals("test", policyGroupById.getName());
    }

    @Test
    void myAttributePolicyGroups() {
        UserDetails userDetails = new org.springframework.security.core.userdetails.User("test@test.com", "",
                Collections.emptyList());

        List<AttributePolicyGroupForm> forms = attributePolicyService.myAttributePolicyGroups(userDetails);
        assertEquals(1, forms.size());
    }

    @Test
    @Transactional
    void createGroupPolicyGroup() throws RAObjectNotFoundException {
        Optional<Account> optionalAccount = accountRepository.findByKeyIdentifierEquals("kidtest1");
        AttributePolicyGroupForm form = new AttributePolicyGroupForm();
        form.setAccountId(optionalAccount.get().getId());
        form.setName("Group 1");

        AttributePolicy staticAttribute = new AttributePolicy();
        staticAttribute.setAttributeName("staticAttr");
        staticAttribute.setStaticValue(true);
        staticAttribute.setAttributeValue("staticVal");

        AttributePolicy dynamicAttribute = new AttributePolicy();
        dynamicAttribute.setAttributeName("dynamicAttr");
        dynamicAttribute.setStaticValue(false);
        dynamicAttribute.setAttributeValue("{fqdn}");

        form.getAttributePolicies().add(staticAttribute);
        form.getAttributePolicies().add(dynamicAttribute);

        Long apgId = attributePolicyService.createGroupPolicyGroup(form);
        assertTrue(apgId > 0);

        Optional<AttributePolicyGroup> apg = attributePolicyGroupRepository.findById(apgId);
        assertEquals(2, apg.get().getAttributePolicies().size());
    }

    @Test
    @Transactional
    void updateGroupPolicyGroup() throws RAObjectNotFoundException {
        Optional<AttributePolicyGroup> optionalGroup = attributePolicyGroupRepository.findDistinctByName("Test Group");
        AttributePolicyGroup group = optionalGroup.get();

        AttributePolicyGroupForm form = new AttributePolicyGroupForm(group);
        AttributePolicy changePolicy = form.getAttributePolicies().get(0);
        changePolicy.setAttributeValue("newStaticValue");

        String attributeName = changePolicy.getAttributeName();

        AttributePolicyGroupForm newForm = attributePolicyService.updateGroupPolicyGroup(form);

        boolean found = false;
        List<AttributePolicy> policies = newForm.getAttributePolicies();
        for(AttributePolicy ap : policies){
            if(ap.getAttributeName().contentEquals(attributeName)){
                found = true;
                assertEquals("newStaticValue", ap.getAttributeValue());
            }
        }

        assertTrue(found);
    }

    @Test
    void deleteAttributePolicyGroup() {
        Optional<AttributePolicyGroup> optionalGroup = attributePolicyGroupRepository.findDistinctByName("Test Group");
        assertTrue(optionalGroup.isPresent());

        attributePolicyService.deleteAttributePolicyGroup(optionalGroup.get().getId());

        optionalGroup = attributePolicyGroupRepository.findDistinctByName("Test Group");
        assertFalse(optionalGroup.isPresent());
    }
}