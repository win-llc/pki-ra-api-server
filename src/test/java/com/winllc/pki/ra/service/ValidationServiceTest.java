package com.winllc.pki.ra.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.winllc.acme.common.AccountValidationResponse;
import com.winllc.acme.common.model.AcmeJWSObject;
import com.winllc.acme.common.model.requestresponse.ExternalAccountBinding;
import com.winllc.acme.common.util.SecurityUtil;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.Domain;
import com.winllc.pki.ra.exception.RAException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.mock.MockUtil;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.DomainRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.winllc.pki.ra.mock.MockUtil.hmacJwk;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class ValidationServiceTest {

    @Autowired
    private ValidationService validationService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private DomainRepository domainRepository;

    private static String testMacKey;

    @BeforeAll
    static void beforeAll(){
        testMacKey = SecurityUtil.generateRandomString(32);
    }

    @BeforeEach
    @Transactional
    void before(){
        Account account = new Account();
        account.setKeyIdentifier("kidtest1");
        account.setMacKey(testMacKey);
        account.setProjectName("Test Project");
        account.setPreAuthorizationIdentifiers(Collections.singleton("test.winllc-dev.com"));
        account = accountRepository.save(account);

        Domain domain = new Domain();
        domain.setBase("winllc-dev.com");
        domain.getCanIssueAccounts().add(account);
        domain = domainRepository.save(domain);

        account.getCanIssueDomains().add(domain);
        accountRepository.save(account);
    }

    @AfterEach
    @Transactional
    void after(){
        accountRepository.deleteAll();
        domainRepository.deleteAll();
    }

    @Test
    void getAccountValidationRules() throws RAObjectNotFoundException {
        AccountValidationResponse validationResponse = validationService.getAccountValidationRules("kidtest1");
        assertEquals(1, validationResponse.getCaValidationRules().size());
    }

    @Test
    void getAccountPreAuthorizedIdentifiers() throws RAObjectNotFoundException {
        Set<String> preAuthz = validationService.getAccountPreAuthorizedIdentifiers("kidtest1");
        assertEquals(1, preAuthz.size());
    }

    @Test
    void verifyExternalAccountBinding() throws JOSEException, JsonProcessingException, ParseException, RAException {
        com.winllc.acme.common.model.requestresponse.AccountRequest accountRequest =
                new com.winllc.acme.common.model.requestresponse.AccountRequest();

        JWSHeader.Builder builder = new JWSHeader.Builder(JWSAlgorithm.HS256)
                .jwk(hmacJwk.toPublicJWK());
        builder.keyID("kidtest1");
        builder.customParam("url", "https://example.com/acme/new-account");

        Payload payload = new Payload(MockUtil.rsaJWK.toJSONObject());

        JWSSigner signer = new MACSigner(testMacKey);
        JWSObject testObj = new JWSObject(builder.build(), payload);
        testObj.sign(signer);

        ExternalAccountBinding eab = new ExternalAccountBinding(testObj);

        accountRequest.setExternalAccountBinding(eab);

        AcmeJWSObject acmeJWSObject = MockUtil.buildCustomAcmeJwsObject(accountRequest, "https://example.com/acme/new-account");
        Boolean validated = validationService.verifyExternalAccountBinding(testMacKey, "kidtest1",
                testObj.serialize(), acmeJWSObject.serialize());

        assertTrue(validated);
    }

    @Test
    void getCanIssueDomains() throws RAObjectNotFoundException {
        List<String> testkid1 = validationService.getCanIssueDomains("kidtest1");
        assertEquals(1, testkid1.size());
    }
}