package com.winllc.pki.ra.service;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.Base64URL;
import com.winllc.acme.common.CAValidationRule;
import com.winllc.pki.ra.beans.AccountRequestForm;
import com.winllc.pki.ra.beans.AccountUpdateForm;
import com.winllc.pki.ra.beans.PocFormEntry;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.Domain;
import com.winllc.pki.ra.domain.PocEntry;
import com.winllc.pki.ra.domain.User;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.DomainRepository;
import com.winllc.pki.ra.repository.PocEntryRepository;
import com.winllc.pki.ra.repository.UserRepository;
import com.winllc.pki.ra.security.RAUser;
import com.winllc.pki.ra.util.AppUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/account")
public class AccountService {

    private static final Logger log = LogManager.getLogger(AccountService.class);

    static String macKey = "2798044239550a105ef8ba7f187c3c0b657dda5a1aa9500dd0956d943bd4e94501961bf84ecc3578c90aa09b5578b5d313a744e48fe7ecf60d20f0ae6d3ebc5e";

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private PocEntryRepository pocEntryRepository;
    @Autowired
    private UserRepository userRepository;

    static {
        System.out.println("System MAC key: "+macKey);
    }

    @PostConstruct
    private void postConstruct(){
        Account testAccount = new Account();
        testAccount.setKeyIdentifier("kidtest");
        testAccount.setMacKey(macKey);

        Domain domain = new Domain();
        domain.setBase("winllc.com");

        testAccount = accountRepository.save(testAccount);

        domain.getCanIssueAccounts().add(testAccount);

        domain = domainRepository.save(domain);

        testAccount.getCanIssueDomains().add(domain);
        accountRepository.save(testAccount);

        User user = new User();
        user.setIdentifier(UUID.randomUUID());
        user.setUsername("test");

        userRepository.save(user);
    }


    @PostMapping("/create")
    public ResponseEntity<?> createNewAccount(@RequestBody AccountRequestForm form){
        String macKey = AppUtil.generate256BitString();
        String keyIdentifier = AppUtil.generate20BitString();

        Account account = new Account();
        account.setKeyIdentifier(keyIdentifier);
        account.setMacKey(macKey);

        account = accountRepository.save(account);

        log.info("Created account with kid: "+account.getKeyIdentifier());
        log.info("Mac Key: "+Base64.getEncoder().encodeToString(account.getMacKey().getBytes()));

        //TODO return both to account holder for entry into ACME client

        return ResponseEntity.status(201).build();
    }

    @PostMapping("/request")
    public ResponseEntity<?> createAccountRequest(@RequestBody AccountRequestForm form){

        //todo

        return ResponseEntity.ok().build();
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateAccount(@RequestBody AccountUpdateForm form) throws Exception {
        //TODO
        if(form.isValid()){
            Optional<Account> optionalAccount = accountRepository.findById(form.getId());
            if(optionalAccount.isPresent()){
                Account account = optionalAccount.get();

                Map<String, PocEntry> existingPocMap = account.getPocs().stream()
                        .collect(Collectors.toMap(p -> p.getEmail(), p -> p));

                for(PocFormEntry email : form.getPocEmails()){

                    //Only create entry if POC email does not exist
                    if(existingPocMap.get(email.getEmail()) == null) {

                        PocEntry pocEntry = new PocEntry();
                        pocEntry.setEnabled(true);
                        pocEntry.setEmail(email.getEmail());
                        pocEntry.setAddedOn(Timestamp.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));
                        pocEntry.setAccount(account);
                        pocEntry = pocEntryRepository.save(pocEntry);

                        account.getPocs().add(pocEntry);
                    }
                }

                accountRepository.save(account);
            }else{
                throw new Exception("Could not find account with ID: "+form.getId());
            }

        }

        return ResponseEntity.status(200).build();
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAll(@AuthenticationPrincipal RAUser raUser){
        log.info("RAUser: "+raUser.getUsername());
        List<Account> accounts = accountRepository.findAll();

        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/findByKeyIdentifier/{kid}")
    public ResponseEntity<?> findByKeyIdentifier(@PathVariable String kid){

        Account account = accountRepository.findByKeyIdentifierEquals(kid);

        return ResponseEntity.ok(account);
    }

    @GetMapping("/byId/{id}")
    public ResponseEntity<?> findById(@PathVariable long id){

        Optional<Account> accountOptional = accountRepository.findById(id);

        if(accountOptional.isPresent()){
            return ResponseEntity.ok(accountOptional.get());
        }else {
            return ResponseEntity.notFound().build();
        }
    }


    @PostMapping("/validationRules/{kid}")
    public ResponseEntity<?> getAccountValidationRules(@PathVariable String kid){
        //todo
        Account account = accountRepository.findByKeyIdentifierEquals(kid);

        List<CAValidationRule> validationRules = new ArrayList<>();

        for(Domain domain : account.getCanIssueDomains()){
            CAValidationRule validationRule = new CAValidationRule();
            validationRule.setAllowHostnameIssuance(true);
            validationRule.setAllowIssuance(true);
            validationRule.setBaseDomainName(domain.getBase());
            validationRule.setIdentifierType("dns");
            validationRule.setRequireHttpChallenge(true);

            validationRules.add(validationRule);
        }

        return ResponseEntity.ok(validationRules);
    }

    @GetMapping("/getAccountPocs/{kid}")
    public ResponseEntity<?> getAccountPocs(@PathVariable String kid){

        Account account = accountRepository.findByKeyIdentifierEquals(kid);
        List<PocEntry> pocEntries = pocEntryRepository.findAllByAccount(account);

        return ResponseEntity.ok(pocEntries);
    }

    @GetMapping("/getCanIssueDomains/{kid}")
    public ResponseEntity<?> getCanIssueDomains(@PathVariable String kid){
        Account account = accountRepository.findByKeyIdentifierEquals(kid);
        List<String> domainList = domainRepository.findAllByCanIssueAccountsContains(account)
                .stream().map(Domain::getBase)
                .collect(Collectors.toList());

        return ResponseEntity.ok(domainList);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyExternalAccountBinding(@RequestParam String macKey, @RequestParam String keyIdentifier,
                                                          @RequestParam String jwsObject, @RequestParam String accountObject){
        Base64URL macKeyBase64 = new Base64URL(macKey);

        System.out.println("MAC Key: "+ macKeyBase64.toString());
        System.out.println("Key Identifier: "+keyIdentifier);

        try {
            Account account = accountRepository.findByKeyIdentifierEquals(keyIdentifier);

            JWSObject jwsObjectParsed = JWSObject.parse(jwsObject);
            JWSObject accountJWSParsed = JWSObject.parse(accountObject);

            try {
                JWSSigner signer = new MACSigner(account.getMacKey());

                JWSObject testObj = new JWSObject(jwsObjectParsed.getHeader(), jwsObjectParsed.getPayload());
                testObj.sign(signer);

                System.out.println("Test signed obj: "+testObj.getSignature().toJSONString());

                if(testObj.getSignature().toString().contentEquals(jwsObjectParsed.getSignature().toString())){
                    System.out.println("Account request verified!");

                    //account.setMacKey(jwsObjectParsed.getSignature().toString());
                    //account.setKeyIdentifier(jwsObjectParsed.getHeader().getKeyID());

                    //accountRepository.save(account);

                    return ResponseEntity.status(200)
                            .build();
                }
            } catch (KeyLengthException e) {
                e.printStackTrace();
            }


        } catch (Exception e) {
            log.error("Could not verify request", e);
        }

        //TODO verify account
        return ResponseEntity.status(403)
                .build();
    }


    private boolean verifyBinding(Account systemAccount, JWSObject accountObject, JWSObject verifyObject){
        JWK accountPublicKey = accountObject.getHeader().getJWK().toPublicJWK();
        JWSHeader verifyHeader = verifyObject.getHeader();

        return false;
    }

}
