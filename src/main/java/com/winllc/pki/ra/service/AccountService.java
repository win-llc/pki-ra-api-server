package com.winllc.pki.ra.service;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.Base64URL;
import com.winllc.acme.common.CAValidationRule;
import com.winllc.pki.ra.beans.*;
import com.winllc.pki.ra.domain.AccountRequest;
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
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/account")
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
        user.setEmail("test@test.com");

        userRepository.save(user);
    }


    @PostMapping("/create")
    public ResponseEntity<?> createNewAccount(@RequestBody AccountRequest form){
        //TODO return both to account holder for entry into ACME client

        Account account = buildNew();
        account.setProjectName(form.getProjectName());

        accountRepository.save(account);

        return ResponseEntity.status(201).build();
    }

    public Account buildNew(){
        String macKey = AppUtil.generate256BitString();
        String keyIdentifier = AppUtil.generate20BitString();

        Account account = new Account();
        account.setKeyIdentifier(keyIdentifier);
        account.setMacKey(macKey);

        account = accountRepository.save(account);

        log.info("Created account with kid: "+account.getKeyIdentifier());
        log.info("Mac Key: "+ Base64.getEncoder().encodeToString(account.getMacKey().getBytes()));
        return account;
    }

    @GetMapping("/myAccounts")
    public ResponseEntity<?> getAccountsForCurrentUser(@AuthenticationPrincipal RAUser raUser){
        Optional<User> optionalUser = userRepository.findOneByUsername(raUser.getUsername());
        if(optionalUser.isPresent()){
            User currentUser = optionalUser.get();
            List<PocEntry> pocEntries = pocEntryRepository.findAllByEmailEquals(currentUser.getEmail());

            List<Account> accounts;
            if(!CollectionUtils.isEmpty(pocEntries)) {
                accounts = accountRepository.findAllByAccountUsersContainsOrPocsContaining(currentUser, pocEntries);
            }else{
                accounts = accountRepository.findAllByAccountUsersContains(currentUser);
            }

            List<AccountInfo> accountInfoList = new ArrayList<>();
            for(Account account : accounts){
                AccountInfo info = buildAccountInfo(account);
                accountInfoList.add(info);
            }

            return ResponseEntity.ok(accountInfoList);
        }else{
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/update")
    @Transactional
    public ResponseEntity<?> updateAccount(@RequestBody AccountUpdateForm form) throws Exception {
        //TODO
        if(form.isValid()){
            Optional<Account> optionalAccount = accountRepository.findById(form.getId());
            if(optionalAccount.isPresent()){
                Account account = optionalAccount.get();

                Map<String, PocEntry> existingPocMap = pocEntryRepository.findAllByAccount(account).stream()
                        .collect(Collectors.toMap(p -> p.getEmail(), p -> p));

                List<String> emailsToRemove = existingPocMap.values()
                        .stream().filter(p -> !form.getPocEmails().contains(new PocFormEntry(p.getEmail())))
                        .map(e -> e.getEmail())
                        .collect(Collectors.toList());

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

                account.getPocs().removeIf(p -> emailsToRemove.contains(p.getEmail()));
                pocEntryRepository.deleteAllByEmailInAndAccountEquals(emailsToRemove, account);

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
            AccountInfo info = buildAccountInfo(accountOptional.get());

            return ResponseEntity.ok(info);
        }else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/info/byId/{id}")
    public ResponseEntity<?> findInfoById(@PathVariable long id){
        Optional<Account> accountOptional = accountRepository.findById(id);

        if(accountOptional.isPresent()){
            Account account = accountOptional.get();

            AccountInfo accountInfo = buildAccountInfo(account);

            return ResponseEntity.ok(accountInfo);
        }else {
            return ResponseEntity.notFound().build();
        }
    }

    private AccountInfo buildAccountInfo(Account account){
        List<Domain> canIssueDomains = domainRepository.findAllByCanIssueAccountsContains(account);
        List<User> accountUsers = userRepository.findAllByAccountsContains(account);

        List<DomainInfo> domainInfoList = canIssueDomains.stream()
                .map(DomainInfo::new)
                .collect(Collectors.toList());

        List<UserInfo> userInfoList = accountUsers.stream()
                .map(UserInfo::new)
                .collect(Collectors.toList());

        AccountInfo accountInfo = new AccountInfo(account);
        accountInfo.setCanIssueDomains(domainInfoList);
        accountInfo.setPocs(userInfoList);

        return accountInfo;
    }


    @GetMapping("/getAccountPocs/{kid}")
    public ResponseEntity<?> getAccountPocs(@PathVariable String kid){

        Account account = accountRepository.findByKeyIdentifierEquals(kid);
        List<PocEntry> pocEntries = pocEntryRepository.findAllByAccount(account);

        return ResponseEntity.ok(pocEntries);
    }


    public ResponseEntity<?> addUserToAccount(){
        //todo
        return null;
    }



    private boolean verifyBinding(Account systemAccount, JWSObject accountObject, JWSObject verifyObject){
        JWK accountPublicKey = accountObject.getHeader().getJWK().toPublicJWK();
        JWSHeader verifyHeader = verifyObject.getHeader();

        return false;
    }

}
