package com.winllc.pki.ra.service;

import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.pki.ra.beans.form.AccountRequestForm;
import com.winllc.pki.ra.endpoint.acme.AcmeServerService;
import com.winllc.pki.ra.beans.PocFormEntry;
import com.winllc.pki.ra.beans.form.AccountUpdateForm;
import com.winllc.pki.ra.beans.info.AccountInfo;
import com.winllc.pki.ra.beans.info.DomainInfo;
import com.winllc.pki.ra.beans.info.UserInfo;
import com.winllc.pki.ra.constants.AuditRecordType;
import com.winllc.pki.ra.domain.*;
import com.winllc.pki.ra.exception.AcmeConnectionException;
import com.winllc.pki.ra.exception.InvalidFormException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.*;
import com.winllc.pki.ra.service.transaction.SystemActionRunner;
import com.winllc.pki.ra.service.validators.AccountRequestValidator;
import com.winllc.pki.ra.service.validators.AccountUpdateValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/account")
public class AccountService extends AbstractService {

    private static final Logger log = LogManager.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final PocEntryRepository pocEntryRepository;
    private final AuditRecordService auditRecordService;
    private final AccountRestrictionRepository accountRestrictionRepository;
    private final AcmeServerManagementService acmeServerManagementService;
    private final AccountRequestValidator accountRequestValidator;
    private final AccountUpdateValidator accountUpdateValidator;

    public AccountService(AccountRepository accountRepository, PocEntryRepository pocEntryRepository,
                          AuditRecordService auditRecordService, AccountRestrictionRepository accountRestrictionRepository,
                          AcmeServerManagementService acmeServerManagementService, ApplicationContext applicationContext,
                          AccountRequestValidator accountRequestValidator, AccountUpdateValidator accountUpdateValidator) {
        super(applicationContext);
        this.accountRepository = accountRepository;
        this.pocEntryRepository = pocEntryRepository;
        this.auditRecordService = auditRecordService;
        this.accountRestrictionRepository = accountRestrictionRepository;
        this.acmeServerManagementService = acmeServerManagementService;
        this.accountRequestValidator = accountRequestValidator;
        this.accountUpdateValidator = accountUpdateValidator;
    }

    @InitBinder("accountRequestForm")
    public void initAccountRequestBinder(WebDataBinder binder) {
        binder.setValidator(accountRequestValidator);
    }

    @InitBinder("accountUpdateForm")
    public void initAccountUpdateBinder(WebDataBinder binder) {
        binder.setValidator(accountUpdateValidator);
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public Long createNewAccount(@Valid @RequestBody AccountRequestForm form) {
        Account account = Account.buildNew(form.getProjectName());

        account = accountRepository.save(account);

        SystemActionRunner.build(context)
                .createAuditRecord(AuditRecordType.ACCOUNT_ADDED, account)
                .execute();

        return account.getId();
    }

    @GetMapping("/myAccounts")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<AccountInfo> getAccountsForCurrentUser(@AuthenticationPrincipal UserDetails raUser)
            throws AcmeConnectionException {

        List<PocEntry> pocEntries = pocEntryRepository.findAllByEmailEquals(raUser.getUsername());

        List<Account> accounts = new ArrayList<>();
        for (PocEntry pocEntry : pocEntries) {
            Account account = pocEntry.getAccount();
            if (account != null) accounts.add(account);
        }

        Set<Account> filtered = new HashSet<>(accounts);

        List<AccountInfo> accountInfoList = new ArrayList<>();
        for (Account account : filtered) {
            AccountInfo info = buildAccountInfo(account);

            //todo fix this section
            AcmeServerService service = acmeServerManagementService.getAcmeServerServiceByName("winllc").get();
            List<DirectoryDataSettings> directoryDataSettings = acmeServerManagementService.getAllDirectorySettings("winllc");

            if (!CollectionUtils.isEmpty(directoryDataSettings)) {
                for (DirectoryDataSettings dds : directoryDataSettings) {
                    AccountInfo.AcmeConnectionInfo connectionInfo = new AccountInfo.AcmeConnectionInfo();
                    connectionInfo.setDirectory(dds.getName());
                    connectionInfo.setAccountKeyId(info.getKeyIdentifier());
                    connectionInfo.setMacKeyBase64(info.getMacKeyBase64());

                    connectionInfo.setUrl(service.getConnection().getConnectionInfo().getUrl() + "/" + dds.getName() + "/directory");
                    info.getAcmeConnectionInfoList().add(connectionInfo);
                }

                accountInfoList.add(info);
            }
        }

        return accountInfoList;
    }

    @PostMapping("/update")
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public AccountInfo updateAccount(@Valid @RequestBody AccountUpdateForm form) throws Exception {
        Optional<Account> optionalAccount = accountRepository.findById(form.getId());
        if (optionalAccount.isPresent()) {
            Account account = optionalAccount.get();

            Map<String, PocEntry> existingPocMap = pocEntryRepository.findAllByAccount(account).stream()
                    .collect(Collectors.toMap(p -> p.getEmail(), p -> p));

            List<String> emailsToRemove = existingPocMap.values()
                    .stream().filter(p -> !form.getPocEmails().contains(new PocFormEntry(p.getEmail())))
                    .map(e -> e.getEmail())
                    .collect(Collectors.toList());

            for (PocFormEntry email : form.getPocEmails()) {

                //Only create entry if POC email does not exist
                if (existingPocMap.get(email.getEmail()) == null) {

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
            return buildAccountInfo(account);
        } else {
            throw new RAObjectNotFoundException(Account.class, form.getId());
        }
    }

    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public List<Account> getAll(@AuthenticationPrincipal UserDetails raUser) {
        log.info("RAUser: " + raUser.getUsername());
        List<Account> accounts = accountRepository.findAll();

        return accounts;
    }

    //todo haspermission
    @GetMapping("/findByKeyIdentifier/{kid}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public AccountInfo findByKeyIdentifier(@PathVariable String kid) throws RAObjectNotFoundException {

        Optional<Account> optionalAccount = accountRepository.findByKeyIdentifierEquals(kid);

        if (optionalAccount.isPresent()) {
            Account account = optionalAccount.get();
            return buildAccountInfo(account);
        } else {
            throw new RAObjectNotFoundException(Account.class, kid);
        }
    }

    @PreAuthorize("hasPermission(#id, 'com.winllc.pki.ra.domain.Account', 'view_account')")
    @GetMapping("/byId/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public AccountInfo findById(@PathVariable long id) throws RAObjectNotFoundException {

        Optional<Account> accountOptional = accountRepository.findById(id);

        if (accountOptional.isPresent()) {
            AccountInfo info = buildAccountInfo(accountOptional.get());

            return info;
        } else {
            throw new RAObjectNotFoundException(Account.class, id);
        }
    }

    @PreAuthorize("hasPermission(#id, 'com.winllc.pki.ra.domain.Account', 'view_account')")
    @GetMapping("/info/byId/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public AccountInfo findInfoById(@PathVariable long id) throws RAObjectNotFoundException {
        Optional<Account> accountOptional = accountRepository.findById(id);

        if (accountOptional.isPresent()) {
            Account account = accountOptional.get();

            AccountInfo accountInfo = buildAccountInfo(account);

            return accountInfo;
        } else {
            throw new RAObjectNotFoundException(Account.class, id);
        }
    }

    @GetMapping("/getAccountPocs/{kid}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<UserInfo> getAccountPocs(@PathVariable String kid) throws RAObjectNotFoundException {

        Optional<Account> optionalAccount = accountRepository.findByKeyIdentifierEquals(kid);

        if (optionalAccount.isPresent()) {
            Account account = optionalAccount.get();
            AccountInfo accountInfo = buildAccountInfo(account);

            return accountInfo.getPocs();
        } else {
            throw new RAObjectNotFoundException(Account.class, kid);
        }
    }

    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void delete(@PathVariable Long id) {

        Optional<Account> optionalAccount = accountRepository.findById(id);

        if (optionalAccount.isPresent()) {
            Account account = optionalAccount.get();
            accountRepository.delete(account);

            auditRecordService.save(AuditRecord.buildNew(AuditRecordType.ACCOUNT_REMOVED, account));
        } else {
            log.debug("Did not delete Account, ID not found: " + id);
        }
    }


    private AccountInfo buildAccountInfo(Account account) {
        List<PocEntry> pocEntries = pocEntryRepository.findAllByAccount(account);
        Set<DomainPolicy> accountDomainPolicies = account.getAccountDomainPolicies();

        List<Domain> canIssueDomains = accountDomainPolicies.stream()
                .filter(dp -> dp.getTargetDomain() != null)
                .map(DomainPolicy::getTargetDomain)
                .collect(Collectors.toList());

        List<DomainInfo> domainInfoList = canIssueDomains.stream()
                .map(d -> new DomainInfo(d, true))
                .collect(Collectors.toList());

        List<UserInfo> userInfoFromPocs = pocEntries.stream()
                .map(UserInfo::new)
                .collect(Collectors.toList());

        Set<UserInfo> userSet = new HashSet<>();
        userSet.addAll(userInfoFromPocs);

        AccountInfo accountInfo = new AccountInfo(account, true);
        accountInfo.setCanIssueDomains(domainInfoList);
        accountInfo.setPocs(new ArrayList<>(userSet));

        return accountInfo;
    }

}
