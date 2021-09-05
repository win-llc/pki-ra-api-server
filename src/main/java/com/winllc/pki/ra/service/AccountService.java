package com.winllc.pki.ra.service;

import com.winllc.acme.common.domain.Account;
import com.winllc.pki.ra.beans.form.AccountRequestForm;
import com.winllc.pki.ra.beans.PocFormEntry;
import com.winllc.pki.ra.beans.form.AccountUpdateForm;
import com.winllc.pki.ra.beans.info.AccountInfo;
import com.winllc.pki.ra.beans.info.DomainInfo;
import com.winllc.acme.common.constants.AuditRecordType;
import com.winllc.acme.common.domain.*;
import com.winllc.pki.ra.exception.AcmeConnectionException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.*;
import com.winllc.pki.ra.service.external.SecurityPolicyServerProjectDetails;
import com.winllc.pki.ra.service.transaction.SystemActionRunner;
import com.winllc.pki.ra.service.validators.AccountRequestValidator;
import com.winllc.pki.ra.service.validators.AccountUpdateValidator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.WebDataBinder;
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
public class AccountService extends AbstractService {

    private static final Logger log = LogManager.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final PocEntryRepository pocEntryRepository;
    private final AuditRecordService auditRecordService;
    private final AcmeServerManagementService acmeServerManagementService;
    private final AccountRequestValidator accountRequestValidator;
    private final AccountUpdateValidator accountUpdateValidator;
    private final AuthCredentialRepository authCredentialRepository;
    @Autowired
    private AuthCredentialService authCredentialService;
    @Autowired
    private SecurityPolicyService securityPolicyService;
    @Autowired
    private AccountRestrictionService accountRestrictionService;
    @Autowired
    private DomainLinkToAccountRequestRepository domainLinkToAccountRequestRepository;

    public AccountService(AccountRepository accountRepository, PocEntryRepository pocEntryRepository,
                          AuditRecordService auditRecordService,
                          AcmeServerManagementService acmeServerManagementService, ApplicationContext applicationContext,
                          AccountRequestValidator accountRequestValidator, AccountUpdateValidator accountUpdateValidator,
                          AuthCredentialRepository authCredentialRepository) {
        super(applicationContext);
        this.accountRepository = accountRepository;
        this.pocEntryRepository = pocEntryRepository;
        this.auditRecordService = auditRecordService;
        this.acmeServerManagementService = acmeServerManagementService;
        this.accountRequestValidator = accountRequestValidator;
        this.accountUpdateValidator = accountUpdateValidator;
        this.authCredentialRepository = authCredentialRepository;
    }

    @InitBinder("accountRequestForm")
    public void initAccountRequestBinder(WebDataBinder binder) {
        binder.setValidator(accountRequestValidator);
    }

    @InitBinder("accountUpdateForm")
    public void initAccountUpdateBinder(WebDataBinder binder) {
        binder.setValidator(accountUpdateValidator);
    }

    @PostConstruct
    @Transactional
    public void init() {
        //if account does not have at least one AuthCred, add one
        for (Account account : accountRepository.findAll()) {
            //Hibernate.initialize(account.getAuthCredentials());
            List<AuthCredential> credentials = authCredentialRepository.findAllByParentEntity(account);
            if (credentials.size() == 0) {
                log.info("Account did not have an AuthCredential, adding: " + account.getProjectName());
                authCredentialService.addNewAuthCredentialToEntry(account);
            }
        }
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public Long createNewAccount(@Valid @RequestBody AccountRequestForm form) throws Exception {
        Account account = Account.buildNew(form.getProjectName());

        account = accountRepository.save(account);

        account = (Account) authCredentialService.addNewAuthCredentialToEntry(account);

        if(StringUtils.isNotBlank(form.getSecurityPolicyServerProjectId())) {
            SecurityPolicyServerProjectDetails projectDetails
                    = securityPolicyService.getProjectDetails(form.getSecurityPolicyServerProjectId());

            if(projectDetails != null) {
                account.setSecurityPolicyServerProjectId(projectDetails.getProjectId());
            }
        }

        account = accountRepository.save(account);

        //add admin user to pocs
        if(StringUtils.isNotBlank(form.getAccountOwnerEmail())) {
            PocFormEntry adminPoc = new PocFormEntry(form.getAccountOwnerEmail());
            adminPoc.setOwner(true);

            pocUpdater(account, Collections.singletonList(adminPoc));
        }

        accountRestrictionService.syncPolicyServerBackedAccountRestrictions(account);

        SystemActionRunner.build(context)
                .createAuditRecord(AuditRecordType.ACCOUNT_ADDED, account)
                .execute();

        return account.getId();
    }


    @GetMapping("/myAccounts")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<AccountInfo> getAccountsForCurrentUser(Authentication authentication)
            throws AcmeConnectionException {

        List<PocEntry> pocEntries = pocEntryRepository.findAllByEmailEquals(authentication.getName());

        List<Account> accounts = new ArrayList<>();
        for (PocEntry pocEntry : pocEntries) {
            Account account = pocEntry.getAccount();
            if (account != null) accounts.add(account);
        }

        Set<Account> filtered = new HashSet<>(accounts);

        List<AccountInfo> accountInfoList = new ArrayList<>();
        for (Account account : filtered) {
            AccountInfo info = buildAccountInfo(account, authentication);

            /*
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

             */
            accountInfoList.add(info);
        }

        return accountInfoList;
    }

    @PostMapping("/update")
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public AccountInfo updateAccount(@Valid @RequestBody AccountUpdateForm form, Authentication authentication) throws Exception {
        Optional<Account> optionalAccount = accountRepository.findById(form.getId());
        if (optionalAccount.isPresent()) {
            Account account = optionalAccount.get();

            account.setSecurityPolicyServerProjectId(form.getSecurityPolicyProjectId());
            account = accountRepository.save(account);

            return buildAccountInfo(account, authentication);
        } else {
            throw new RAObjectNotFoundException(Account.class, form.getId());
        }
    }

    @GetMapping("/getAccountPocs/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<PocFormEntry> getAccountPocs(@PathVariable Long id, Authentication authentication) throws RAObjectNotFoundException {

        Optional<Account> optionalAccount = accountRepository.findById(id);

        if (optionalAccount.isPresent()) {
            Account account = optionalAccount.get();
            AccountInfo accountInfo = buildAccountInfo(account, authentication);

            return accountInfo.getPocs();
        } else {
            throw new RAObjectNotFoundException(Account.class, id);
        }
    }

    @GetMapping("/getAccountPocOptions/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public Map<Long, String> getAccountPocOptions(@PathVariable Long id, Authentication authentication) throws RAObjectNotFoundException {
        List<PocFormEntry> pocs = getAccountPocs(id, authentication);
        return pocs.stream()
                .collect(Collectors.toMap(d -> d.getId(), d -> d.getEmail()));
    }

    @PostMapping("/updatePocs/{accountId}")
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    public List<PocFormEntry> updateAccountPocs(@PathVariable(name = "accountId") Long accountId, @RequestBody List<PocFormEntry> pocs, Authentication authentication)
            throws RAObjectNotFoundException {
        Optional<Account> optionalAccount = accountRepository.findById(accountId);
        if (optionalAccount.isPresent()) {
            Account account = optionalAccount.get();

            pocUpdater(account, pocs);
            return getAccountPocs(accountId, authentication);
        } else {
            throw new RAObjectNotFoundException(Account.class, accountId);
        }
    }

    public Account pocUpdater(Account account, List<PocFormEntry> pocs){
        Map<String, PocEntry> existingPocMap = pocEntryRepository.findAllByAccount(account).stream()
                .collect(Collectors.toMap(p -> p.getEmail(), p -> p));

        List<String> emailsToRemove = existingPocMap.values()
                .stream().filter(p -> !pocs.contains(new PocFormEntry(p.getEmail())))
                .map(e -> e.getEmail())
                .collect(Collectors.toList());

        //delete existing if removed
        account.getPocs().removeIf(p -> emailsToRemove.contains(p.getEmail()));
        pocEntryRepository.deleteAllByEmailInAndAccountEquals(emailsToRemove, account);
        accountRepository.save(account);

        //add new or update
        if(!CollectionUtils.isEmpty(pocs)) {
            for (PocFormEntry email : pocs) {

                //Only create entry if POC email does not exist
                if (existingPocMap.get(email.getEmail()) == null) {

                    account = addPocToAccount(account, email);
                } else {
                    PocEntry pocEntry = existingPocMap.get(email.getEmail());
                    pocEntry.setOwner(email.isOwner());
                    pocEntry.setCanManageAllServers(email.isCanManageAllServers());
                    pocEntryRepository.save(pocEntry);
                }
            }
        }

        return accountRepository.save(account);
    }

    public Account addPocToAccount(Account account, PocFormEntry poc){
        Optional<PocEntry> optionalPoc = pocEntryRepository.findDistinctByEmailEqualsAndAccount(poc.getEmail(), account);

        if(optionalPoc.isEmpty()) {
            PocEntry pocEntry = new PocEntry();
            pocEntry.setEnabled(true);
            pocEntry.setEmail(poc.getEmail());
            pocEntry.setAddedOn(Timestamp.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));
            pocEntry.setAccount(account);
            pocEntry.setOwner(poc.isOwner());
            poc.setCanManageAllServers(poc.isCanManageAllServers());
            pocEntry = pocEntryRepository.save(pocEntry);

            account.getPocs().add(pocEntry);
            return accountRepository.save(account);
        }else{
            return account;
        }
    }

    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<AccountInfo> getAll(@AuthenticationPrincipal UserDetails raUser) {
        log.info("RAUser: " + raUser.getUsername());
        List<Account> accounts = accountRepository.findAll();

        return accounts.stream()
                .map(a -> new AccountInfo(a, false))
                .collect(Collectors.toList());
    }

    //todo haspermission
    @GetMapping("/findByKeyIdentifier/{kid}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public AccountInfo findByKeyIdentifier(@PathVariable String kid, Authentication authentication) throws RAObjectNotFoundException {

        Optional<Account> optionalAccount = authCredentialService.getAssociatedAccount(kid);

        if (optionalAccount.isPresent()) {
            Account account = optionalAccount.get();
            return buildAccountInfo(account, authentication);
        } else {
            throw new RAObjectNotFoundException(Account.class, kid);
        }
    }

    public Optional<Account> getByKeyIdentifier(String kid) {
        try {
            return authCredentialService.getAssociatedAccount(kid);
        } catch (RAObjectNotFoundException e) {
            return Optional.empty();
        }
    }

    @PreAuthorize("hasPermission(#id, 'com.winllc.acme.common.domain.Account', 'view_account')")
    @GetMapping("/byId/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public AccountInfo findById(@PathVariable long id, Authentication authentication) throws RAObjectNotFoundException {

        Optional<Account> accountOptional = accountRepository.findById(id);

        if (accountOptional.isPresent()) {
            AccountInfo info = buildAccountInfo(accountOptional.get(), authentication);

            return info;
        } else {
            throw new RAObjectNotFoundException(Account.class, id);
        }
    }

    @PreAuthorize("hasPermission(#id, 'com.winllc.acme.common.domain.Account', 'view_account')")
    @GetMapping("/info/byId/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public AccountInfo findInfoById(@PathVariable long id, Authentication authentication) throws RAObjectNotFoundException {
        Optional<Account> accountOptional = accountRepository.findById(id);

        if (accountOptional.isPresent()) {
            Account account = accountOptional.get();

            AccountInfo accountInfo = buildAccountInfo(account, authentication);

            return accountInfo;
        } else {
            throw new RAObjectNotFoundException(Account.class, id);
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

            //clean up domain link requests
            List<DomainLinkToAccountRequest> existingDomainLinkRequests = domainLinkToAccountRequestRepository.findAllByAccountId(id);
            for(DomainLinkToAccountRequest request : existingDomainLinkRequests){
                domainLinkToAccountRequestRepository.delete(request);
            }
        } else {
            log.debug("Did not delete Account, ID not found: " + id);
        }
    }

/*
    public Page<AccountInfoItem> getPaged(){

    }

    private static Specification<CachedCertificate> buildSearch() {

        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            Predicate onlyLatest = builder.equal(root.get("latestForDn"), true);
            predicates.add(onlyLatest);

        }
    }

 */


    private AccountInfo buildAccountInfo(Account account, Authentication authentication) {
        List<PocEntry> pocEntries = pocEntryRepository.findAllByAccount(account);
        Set<DomainPolicy> accountDomainPolicies = account.getAccountDomainPolicies();

        List<Domain> canIssueDomains = accountDomainPolicies.stream()
                .filter(dp -> dp.getTargetDomain() != null)
                .map(DomainPolicy::getTargetDomain)
                .collect(Collectors.toList());

        List<DomainInfo> domainInfoList = canIssueDomains.stream()
                .map(d -> new DomainInfo(d, true))
                .collect(Collectors.toList());

        List<PocFormEntry> userInfoFromPocs = pocEntries.stream()
                .map(p -> {
                    PocFormEntry entry = new PocFormEntry(p.getEmail());
                    entry.setId(p.getId());
                    entry.setOwner(p.isOwner());
                    entry.setCanManageAllServers(p.isCanManageAllServers());
                    return entry;
                })
                .collect(Collectors.toList());

        Set<PocFormEntry> userSet = new HashSet<>();
        userSet.addAll(userInfoFromPocs);

        AccountInfo accountInfo = new AccountInfo(account, true);
        accountInfo.setCanIssueDomains(domainInfoList);
        accountInfo.setPocs(new ArrayList<>(userSet));

        if(org.apache.commons.collections.CollectionUtils.isNotEmpty(pocEntries)){
            Optional<PocEntry> pocOptional = pocEntries.stream()
                    .filter(e -> e.getEmail().equalsIgnoreCase(authentication.getName()))
                    .findFirst();

            if(pocOptional.isPresent()){
                PocEntry pocEntry = pocOptional.get();
                if(pocEntry.isOwner()) accountInfo.setUserIsOwner(true);
            }
        }

        return accountInfo;
    }

}
