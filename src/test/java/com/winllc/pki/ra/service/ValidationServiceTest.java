package com.winllc.pki.ra.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.winllc.acme.common.CertIssuanceValidationResponse;
import com.winllc.acme.common.domain.Account;
import com.winllc.acme.common.model.AcmeJWSObject;
import com.winllc.acme.common.model.requestresponse.ExternalAccountBinding;
import com.winllc.pki.ra.BaseTest;
import com.winllc.pki.ra.beans.form.AccountRequestForm;
import com.winllc.acme.common.domain.*;
import com.winllc.pki.ra.exception.RAException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.mock.MockUtil;
import com.winllc.acme.common.repository.AccountRepository;
import com.winllc.acme.common.repository.DomainPolicyRepository;
import com.winllc.acme.common.repository.DomainRepository;
import com.winllc.acme.common.repository.ServerEntryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;

import java.text.ParseException;
import java.util.*;

import static com.winllc.pki.ra.mock.MockUtil.hmacJwk;
import static org.junit.jupiter.api.Assertions.*;

class ValidationServiceTest extends BaseTest {

    @Autowired
    private ValidationService validationService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountService accountService;
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private DomainPolicyRepository domainPolicyRepository;
    @Autowired
    private ServerEntryRepository serverEntryRepository;
    @Autowired
    private AuthCredentialService authCredentialService;

    @BeforeEach
    @Transactional
    void before() throws Exception {

        AccountRequestForm form = new AccountRequestForm();
        form.setProjectName("Test Project");

        Long accountId = accountService.createNewAccount(form);

        Account account = accountRepository.findById(accountId).get();
        account.setKeyIdentifier("kidtest1");
        //account.setMacKey(testMacKey);
        account = accountRepository.save(account);

        ServerEntry serverEntry = ServerEntry.buildNew();
        serverEntry.setFqdn("test.winllc-dev.com");
        serverEntry.setAcmeAllowPreAuthz(true);
        serverEntry.setAccount(account);

        serverEntry = serverEntryRepository.save(serverEntry);

        account.getServerEntries().add(serverEntry);
        account = accountRepository.save(account);

        Domain domain = new Domain();
        domain.setBase("winllc-dev.com");
        domain = domainRepository.save(domain);

        DomainPolicy domainPolicy = new DomainPolicy(domain);
        domainPolicy.setAccount(account);
        domainPolicy = domainPolicyRepository.save(domainPolicy);
        account.getAccountDomainPolicies().add(domainPolicy);

        accountRepository.save(account);
    }

    @AfterEach
    @Transactional
    void after(){
        accountRepository.deleteAll();
        domainRepository.deleteAll();
    }

    @Test
    @Transactional
    void getAccountValidationRules() throws RAException {
        Account account = accountRepository.findDistinctByProjectName("Test Project").get();
        AuthCredential authCredential = account.getLatestAuthCredential().get();

        CertIssuanceValidationResponse validationResponse = validationService.getAccountValidationRules(authCredential.getKeyIdentifier());
        assertEquals(1, validationResponse.getCertIssuanceValidationRules().size());
    }

    @Test
    @Transactional
    void getAccountPreAuthorizedIdentifiers() throws RAObjectNotFoundException {
        Account account = accountRepository.findDistinctByProjectName("Test Project").get();
        Optional<AuthCredential> latestAuthCredentialForAccount = authCredentialService.getLatestAuthCredentialForAccount(account);

        Set<String> preAuthz = validationService.getAccountPreAuthorizedIdentifiers(latestAuthCredentialForAccount.get().getKeyIdentifier());
        assertEquals(1, preAuthz.size());
    }

    @Test
    @Transactional
    void verifyExternalAccountBinding() throws JOSEException, JsonProcessingException, ParseException, RAException {
        com.winllc.acme.common.model.requestresponse.AccountRequest accountRequest =
                new com.winllc.acme.common.model.requestresponse.AccountRequest();

        Account account = accountRepository.findDistinctByProjectName("Test Project").get();
        AuthCredential authCredential = account.getAuthCredentials().toArray(new AuthCredential[0])[0];

        JWSHeader.Builder builder = new JWSHeader.Builder(JWSAlgorithm.HS256)
                .jwk(hmacJwk.toPublicJWK());
        builder.keyID("kidtest1");
        builder.customParam("url", "https://example.com/acme/new-account");

        Payload payload = new Payload(MockUtil.rsaJWK.toJSONObject());

        JWSSigner signer = new MACSigner(authCredential.getMacKey());
        JWSObject testObj = new JWSObject(builder.build(), payload);
        testObj.sign(signer);

        ExternalAccountBinding eab = new ExternalAccountBinding(testObj);

        accountRequest.setExternalAccountBinding(eab);

        AcmeJWSObject acmeJWSObject = MockUtil.buildCustomAcmeJwsObject(accountRequest, "https://example.com/acme/new-account");
        Boolean validated = validationService.verifyExternalAccountBinding(authCredential.getMacKey(), authCredential.getKeyIdentifier(),
                testObj.serialize(), acmeJWSObject.serialize());

        assertTrue(validated);
    }

    @Test
    @Transactional
    void getCanIssueDomains() throws Exception {
        Account account = accountRepository.findDistinctByProjectName("Test Project").get();
        AuthCredential authCredential = account.getLatestAuthCredential().get();

        List<String> testkid1 = validationService.getCanIssueDomains(authCredential.getKeyIdentifier());
        assertEquals(1, testkid1.size());
    }
}