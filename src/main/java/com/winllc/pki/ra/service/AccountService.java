package com.winllc.pki.ra.service;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.jwk.JWK;
import com.winllc.pki.ra.beans.*;
import com.winllc.pki.ra.beans.form.AccountUpdateForm;
import com.winllc.pki.ra.beans.info.AccountInfo;
import com.winllc.pki.ra.beans.info.DomainInfo;
import com.winllc.pki.ra.beans.info.UserInfo;
import com.winllc.pki.ra.domain.*;
import com.winllc.pki.ra.repository.*;
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
import javax.validation.Valid;
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
    @Autowired
    private AccountRestrictionRepository accountRestrictionRepository;


    @PostMapping("/create")
    public ResponseEntity<?> createNewAccount(@Valid @RequestBody AccountRequest form){
        //TODO return both to account holder for entry into ACME client

        Account account = buildNew();
        account.setProjectName(form.getProjectName());

        account = accountRepository.save(account);

        return ResponseEntity.ok(String.valueOf(account.getId()));
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
            List<PocEntry> pocEntries = pocEntryRepository.findAllByEmailEquals(currentUser.getUsername());

            List<Account> accounts;
            if(!CollectionUtils.isEmpty(pocEntries)) {
                accounts = accountRepository.findAllByAccountUsersContainsOrPocsIn(currentUser, pocEntries);
            }else{
                accounts = accountRepository.findAllByAccountUsersContains(currentUser);
            }

            Set<Account> filtered = new HashSet<>(accounts);

            List<AccountInfo> accountInfoList = new ArrayList<>();
            for(Account account : filtered){
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
    public ResponseEntity<?> updateAccount(@Valid @RequestBody AccountUpdateForm form) throws Exception {

        if(form.isValid()){
            Optional<Account> optionalAccount = accountRepository.findById(form.getId());
            if(optionalAccount.isPresent()){
                Account account = optionalAccount.get();
                account.setAcmeRequireHttpValidation(form.isAcmeRequireHttpValidation());

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

                account = accountRepository.save(account);
                return ResponseEntity.ok(new AccountInfo(account, true));
            }else{
                throw new Exception("Could not find account with ID: "+form.getId());
            }
        }

        return ResponseEntity.status(500).build();
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAll(@AuthenticationPrincipal RAUser raUser){
        log.info("RAUser: "+raUser.getUsername());
        List<Account> accounts = accountRepository.findAll();

        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/findByKeyIdentifier/{kid}")
    public ResponseEntity<?> findByKeyIdentifier(@PathVariable String kid){

        Optional<Account> optionalAccount = accountRepository.findByKeyIdentifierEquals(kid);

        if(optionalAccount.isPresent()){
            Account account = optionalAccount.get();
            return ResponseEntity.ok(buildAccountInfo(account));
        }else{
            return ResponseEntity.notFound().build();
        }
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

    @GetMapping("/getAccountPocs/{kid}")
    public ResponseEntity<?> getAccountPocs(@PathVariable String kid){

        Optional<Account> optionalAccount = accountRepository.findByKeyIdentifierEquals(kid);

        if(optionalAccount.isPresent()){
            Account account = optionalAccount.get();
            AccountInfo accountInfo = buildAccountInfo(account);

            return ResponseEntity.ok(accountInfo.getPocs());
        }else{
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id){

        accountRepository.deleteById(id);

        return ResponseEntity.ok().build();
    }


    private AccountInfo buildAccountInfo(Account account){
        List<Domain> canIssueDomains = domainRepository.findAllByCanIssueAccountsContains(account);
        List<User> accountUsers = userRepository.findAllByAccountsContains(account);
        List<PocEntry> pocEntries = pocEntryRepository.findAllByAccount(account);

        List<DomainInfo> domainInfoList = canIssueDomains.stream()
                .map(d -> new DomainInfo(d, false))
                .collect(Collectors.toList());

        List<UserInfo> userInfoList = accountUsers.stream()
                .map(UserInfo::new)
                .collect(Collectors.toList());

        List<UserInfo> userInfoFromPocs = pocEntries.stream()
                .map(UserInfo::new)
                .collect(Collectors.toList());

        Set<UserInfo> userSet = new HashSet<>();
        userSet.addAll(userInfoList);
        userSet.addAll(userInfoFromPocs);

        AccountInfo accountInfo = new AccountInfo(account, true);
        accountInfo.setCanIssueDomains(domainInfoList);
        accountInfo.setPocs(new ArrayList<>(userSet));

        return accountInfo;
    }


    private List<AccountRestriction> getAllNotCompletedAccountRestrictions(Account account){
        return accountRestrictionRepository.findAllByAccountAndCompleted(account, false);
    }

    private List<AccountRestriction> getAllNotCompletedAndOverdueAccountRestrictions(Account account){
        return accountRestrictionRepository.findAllByAccountAndDueByBeforeAndCompletedEquals(account, Timestamp.valueOf(LocalDateTime.now()), false);
    }

    private boolean verifyBinding(Account systemAccount, JWSObject accountObject, JWSObject verifyObject){
        JWK accountPublicKey = accountObject.getHeader().getJWK().toPublicJWK();
        JWSHeader verifyHeader = verifyObject.getHeader();

        return false;
    }

}
