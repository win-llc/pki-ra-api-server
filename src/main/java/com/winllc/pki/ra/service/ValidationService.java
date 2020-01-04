package com.winllc.pki.ra.service;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.util.Base64URL;
import com.winllc.acme.common.CAValidationRule;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.Domain;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.DomainRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.provider.HibernateUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/validation")
public class ValidationService {

    private static final Logger log = LogManager.getLogger(ValidationService.class);

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private DomainRepository domainRepository;

    @PostMapping("/rules/{kid}")
    public ResponseEntity<?> getAccountValidationRules(@PathVariable String kid){
        Account account = accountRepository.findByKeyIdentifierEquals(kid);
        List<Domain> allByCanIssueAccountsContains = domainRepository.findAllByCanIssueAccountsContains(account);

        List<CAValidationRule> validationRules = new ArrayList<>();

        //TODO fix this
        for(Domain domain : allByCanIssueAccountsContains){
            CAValidationRule validationRule = new CAValidationRule();
            validationRule.setAllowHostnameIssuance(true);
            validationRule.setAllowIssuance(true);
            validationRule.setBaseDomainName(domain.getBase());
            validationRule.setIdentifierType("dns");
            validationRule.setRequireHttpChallenge(account.isAcmeRequireHttpValidation());

            validationRules.add(validationRule);
        }

        return ResponseEntity.ok(validationRules);
    }

    @GetMapping("/account/preAuthzIdentifiers/{kid}")
    @Transactional
    public ResponseEntity<?> getAccountPreAuthorizedIdentifiers(@PathVariable String kid){
        Account account = accountRepository.findByKeyIdentifierEquals(kid);

        Hibernate.initialize(account.getPreAuthorizationIdentifiers());

        return ResponseEntity.ok(account.getPreAuthorizationIdentifiers());
    }

    @PostMapping("/account/verify")
    public ResponseEntity<?> verifyExternalAccountBinding(@RequestParam String macKey, @RequestParam String keyIdentifier,
                                                          @RequestParam String jwsObject, @RequestParam String accountObject){
        Base64URL macKeyBase64 = new Base64URL(macKey);

        log.info("MAC Key: "+ macKeyBase64.toString());
        log.info("Key Identifier: "+keyIdentifier);

        try {
            Account account = accountRepository.findByKeyIdentifierEquals(keyIdentifier);

            JWSObject jwsObjectParsed = JWSObject.parse(jwsObject);
            JWSObject accountJWSParsed = JWSObject.parse(accountObject);

            try {
                JWSSigner signer = new MACSigner(account.getMacKey());

                JWSObject testObj = new JWSObject(jwsObjectParsed.getHeader(), jwsObjectParsed.getPayload());
                testObj.sign(signer);

                log.info("Test signed obj: "+testObj.getSignature().toJSONString());

                if(testObj.getSignature().toString().contentEquals(jwsObjectParsed.getSignature().toString())){
                    log.info("Account request verified!");

                    return ResponseEntity.status(200)
                            .build();
                }
            } catch (KeyLengthException e) {
                log.error("Invalid key length", e);
            }


        } catch (Exception e) {
            log.error("Could not verify request", e);
        }

        //TODO verify account
        return ResponseEntity.status(403)
                .build();
    }

    @GetMapping("/account/getCanIssueDomains/{kid}")
    public ResponseEntity<?> getCanIssueDomains(@PathVariable String kid){
        Account account = accountRepository.findByKeyIdentifierEquals(kid);
        List<String> domainList = domainRepository.findAllByCanIssueAccountsContains(account)
                .stream().map(Domain::getBase)
                .collect(Collectors.toList());

        return ResponseEntity.ok(domainList);
    }
}
