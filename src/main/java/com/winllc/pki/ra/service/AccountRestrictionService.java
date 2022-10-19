package com.winllc.pki.ra.service;

import com.winllc.acme.common.domain.Account;
import com.winllc.pki.ra.beans.PocFormEntry;
import com.winllc.pki.ra.beans.form.AccountRestrictionForm;
import com.winllc.pki.ra.beans.form.DomainPolicyForm;
import com.winllc.acme.common.constants.AccountRestrictionAction;
import com.winllc.acme.common.constants.AccountRestrictionType;
import com.winllc.pki.ra.constants.ServerSettingRequired;
import com.winllc.acme.common.domain.*;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.AccountRepository;
import com.winllc.acme.common.repository.AccountRestrictionRepository;
import com.winllc.acme.common.repository.DomainRepository;
import com.winllc.pki.ra.service.transaction.SystemActionRunner;
import com.winllc.pki.ra.service.transaction.ThrowingSupplier;
import com.winllc.pki.ra.service.validators.AccountRestrictionValidator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accountRestriction")
public class AccountRestrictionService extends
        AccountDataTableService<AccountRestriction, AccountRestrictionForm> {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    private static final Logger log = LogManager.getLogger(AccountRestrictionService.class);

    private final AccountRepository accountRepository;
    private final AccountRestrictionRepository accountRestrictionRepository;
    private final AccountRestrictionValidator accountRestrictionValidator;
    @Autowired
    private SecurityPolicyService securityPolicyService;
    @Autowired
    private ServerSettingsService serverSettingsService;
    //@Autowired
    //private AccountService accountService;
    @Autowired
    private DomainPolicyService domainPolicyService;
    @Autowired
    private DomainRepository domainRepository;


    public AccountRestrictionService(AccountRepository accountRepository,
                                     AccountRestrictionRepository accountRestrictionRepository,
                                     AccountRestrictionValidator accountRestrictionValidator,
                                     ApplicationContext applicationContext) {
        super(applicationContext, accountRepository, accountRestrictionRepository);
        this.accountRepository = accountRepository;
        this.accountRestrictionRepository = accountRestrictionRepository;
        this.accountRestrictionValidator = accountRestrictionValidator;
    }

    @InitBinder("accountRestrictionForm")
    public void initAccountRequestUpdateBinder(WebDataBinder binder) {
        binder.setValidator(accountRestrictionValidator);
    }

    @Transactional
    public void syncPolicyServerBackedAccountRestrictions(Account account, AccountService accountService){
        //todo

        if(StringUtils.isNotBlank(account.getSecurityPolicyServerProjectId())){
            try {
                final Account[] temp = {account};

                Map<String, List<String>> projectAttributes = securityPolicyService.getProjectAttributes(account.getSecurityPolicyServerProjectId());

                Optional<String> optionalDomainsAttribute = serverSettingsService.getServerSettingValue(ServerSettingRequired.POLICY_SERVER_LDAP_DOMAINSATTRIBUTE);
                optionalDomainsAttribute.ifPresent(a -> {
                    if(projectAttributes.get(a) != null){
                        List<String> domains = projectAttributes.get(a);
                        temp[0] = updateDomainsForAccount(temp[0], domains);
                    }
                });

                Optional<String> optionalPocsAttribute = serverSettingsService.getServerSettingValue(ServerSettingRequired.POLICY_SERVER_LDAP_POCSATTRIBUTE);
                optionalPocsAttribute.ifPresent(a -> {
                    if(projectAttributes.get(a) != null){
                        List<String> pocs = projectAttributes.get(a);
                        temp[0] = updatePocsForAccount(temp[0], pocs, accountService);
                    }
                });

                Optional<String> optionalEnabledAttribute = serverSettingsService.getServerSettingValue(ServerSettingRequired.POLICY_SERVER_LDAP_ENABLEDATTRIBUTE);
                optionalEnabledAttribute.ifPresent(a -> {
                    if (projectAttributes.get(a) != null) {
                        List<String> enabled = projectAttributes.get(a);
                        if(CollectionUtils.isNotEmpty(enabled)){
                            temp[0] = updateEnabledForAccount(temp[0], Boolean.parseBoolean(enabled.get(0)));
                        }
                    }
                });

            } catch (Exception e) {
                log.error("Could not get project attributes", e);
            }
        }

        //todo get pocs
        //todo get domains
        //todo get validity period
        //todo get enabled
        /*
         POLICY_SERVER_LDAP_DOMAINSATTRIBUTE("policyServerLdapDomainsAttribute", "Domains Attribute", "Policy Server", false),
    POLICY_SERVER_LDAP_POCSATTRIBUTE("policyServerLdapPocsAttribute", "POCs Attribute", "Policy Server", false),
    POLICY_SERVER_LDAP_VALIDFROMATTRIBUTE("policyServerValidFromAttribute", "Valid From Attribute", "Policy Server", false),
    POLICY_SERVER_LDAP_VALIDTOATTRIBUTE("policyServerValidToAttribute", "Valid To Attribute", "Policy Server", false),
    POLICY_SERVER_LDAP_ENABLEDATTRIBUTE("policyServerEnabledAttribute", "Enabled Attribute", "Policy Server", false),
         */
    }

    public Account updatePocsForAccount(Account account, List<String> pocs, AccountService accountService){
        //todo only add, don't overwrite
        for(String email : pocs){
            accountService.addPocToAccount(account, new PocFormEntry(email));
        }

        return accountRepository.findById(account.getId()).get();
        //return accountService.pocUpdater(account, pocs.stream().map(PocFormEntry::new).collect(Collectors.toList()));
    }

    public Account updateDomainsForAccount(Account account, List<String> domains){
        //todo
        for(String domain : domains) {
            Optional<Domain> optionalDomain = domainRepository.findDistinctByFullDomainNameEquals(domain);
            if(optionalDomain.isPresent()){
                //only if domain exists in system, don't create new domain
                Domain found = optionalDomain.get();

                DomainPolicyForm form = new DomainPolicyForm();
                form.setAcmeRequireDnsValidation(false);
                form.setAcmeRequireDnsValidation(false);
                form.setDomainId(found.getId());
                form.setAllowIssuance(true);
                try {
                    domainPolicyService.addForType("account", account.getId(), form);
                } catch (Exception e) {
                    log.error("Could not add domain policy", e);
                }
            }
        }

        return account;
    }

    public Account updateEnabledForAccount(Account account, boolean enabled){
        account.setEnabled(enabled);
        return accountRepository.save(account);
    }

    @GetMapping("/types/options")
    public Map<String, String> optionsTypes(){
        return Arrays.asList(AccountRestrictionType.values()).stream()
                .collect(Collectors.toMap(d -> d.toString(), d -> d.toString()));
    }

    @GetMapping("/actions/options")
    public Map<String, String> optionsActions(){
        return Arrays.asList(AccountRestrictionAction.values()).stream()
                .collect(Collectors.toMap(d -> d.toString(), d -> d.toString()));
    }

    @GetMapping("/types")
    @ResponseStatus(HttpStatus.OK)
    public List<AccountRestrictionType> getRestrictionTypes(){
        return Arrays.asList(AccountRestrictionType.values());
    }

    @GetMapping("/actions")
    @ResponseStatus(HttpStatus.OK)
    public List<AccountRestrictionAction> getRestrictionActions(){
        return Arrays.asList(AccountRestrictionAction.values());
    }

    @PreAuthorize("hasPermission(#id, 'com.winllc.acme.common.domain.AccountRestriction', 'view_account_restriction')")
    @GetMapping("/byId/{id}")
    @ResponseStatus(HttpStatus.OK)
    public AccountRestrictionForm getById(@PathVariable Long id) throws RAObjectNotFoundException {
        Optional<AccountRestriction> optionalAccountRestriction = accountRestrictionRepository.findById(id);
        if(optionalAccountRestriction.isPresent()){
            AccountRestriction restriction = optionalAccountRestriction.get();

            return new AccountRestrictionForm(restriction);
        }else{
            throw new RAObjectNotFoundException(AccountRestriction.class, id);
        }
    }

    @PreAuthorize("hasPermission(#form, 'add_account_restriction')")
    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public Long create(@Valid @RequestBody AccountRestrictionForm form) throws Exception {

        final AccountRestriction accountRestriction = formToRestriction(form);

        Notification notification = Notification.buildNew();
        notification.setMessage("Account Restriction Task");

        SystemActionRunner runner = SystemActionRunner.build(context);
        runner.createNotification(notification)
                .markEntityAsTask(accountRestriction.getDueBy().toLocalDateTime().atZone(ZoneId.systemDefault()));

        ThrowingSupplier<AccountRestriction, Exception> action = () -> {
            AccountRestriction saved = accountRestrictionRepository.save(accountRestriction);

            log.info("Account restriction added ID: " + saved.getId());
            return saved;
        };

        AccountRestriction added = runner.execute(action);

        return added.getId();
    }

    @PreAuthorize("hasPermission(#form, 'update_account_restriction')")
    @PostMapping("/update")
    @ResponseStatus(HttpStatus.OK)
    public AccountRestriction update(@Valid @RequestBody AccountRestrictionForm form) throws Exception {

        Optional<AccountRestriction> optionalAccountRestriction = accountRestrictionRepository.findById(form.getId());

        if(optionalAccountRestriction.isPresent()){

            ThrowingSupplier<AccountRestriction, Exception> action = () -> {
                AccountRestriction existing = optionalAccountRestriction.get();
                form.setAccountId(existing.getAccount().getId());
                AccountRestriction fromForm = formToRestriction(form);

                existing.setDueBy(fromForm.getDueBy());
                existing.setCompleted(form.isCompleted());
                if(StringUtils.isNotBlank(form.getAction())) {
                    existing.setAction(AccountRestrictionAction.valueOf(form.getAction()));
                }
                if(StringUtils.isNotBlank(form.getType())) {
                    existing.setType(AccountRestrictionType.valueOf(form.getType()));
                }

                return accountRestrictionRepository.save(existing);
            };

            SystemActionRunner runner = SystemActionRunner.build(this.context);
            AccountRestriction restriction = runner.execute(action);

            return restriction;
        }else{
            throw new RAObjectNotFoundException(form);
        }
    }

    @PreAuthorize("hasPermission(#id, 'com.winllc.acme.common.domain.AccountRestriction', 'delete_account_restriction')")
    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void delete(@PathVariable Long id){
        accountRestrictionRepository.deleteById(id);
    }

    @PreAuthorize("hasPermission(#accountId, 'com.winllc.acme.common.domain.AccountRestriction', 'view_account_restriction')")
    @GetMapping("/allForAccount/{accountId}")
    @ResponseStatus(HttpStatus.OK)
    public List<AccountRestrictionForm> getAllForAccount(@PathVariable Long accountId) throws RAObjectNotFoundException {
        Optional<Account> optionalAccount = accountRepository.findById(accountId);
        if(optionalAccount.isPresent()){
            Account account = optionalAccount.get();

            List<AccountRestriction> allByAccount = accountRestrictionRepository.findAllByAccount(account);

            return allByAccount.stream()
            .map(AccountRestrictionForm::new)
            .collect(Collectors.toList());
        }else{
            throw new RAObjectNotFoundException(Account.class, accountId);
        }
    }

    public boolean checkIfAccountValid(Account account){
        if(!account.isEnabled()) return false;

        //todo check restrictions

        return true;
    }

    private AccountRestriction formToRestriction(AccountRestrictionForm form) throws RAObjectNotFoundException {
        AccountRestriction restriction = new AccountRestriction();
        Optional<Account> optionalAccount = accountRepository.findById(form.getAccountId());
        if(optionalAccount.isPresent()){
            Account account = optionalAccount.get();
            restriction.setAccount(account);

            if(StringUtils.isNotBlank(form.getType())) {
                AccountRestrictionType type = AccountRestrictionType.valueOf(form.getType());
                restriction.setType(type);
            }
            if(StringUtils.isNotBlank(form.getAction())) {
                AccountRestrictionAction action = AccountRestrictionAction.valueOf(form.getAction());
                restriction.setAction(action);
            }

            LocalDateTime localDateTime = LocalDateTime.from(formatter.parse(form.getDueBy()));
            restriction.setDueBy(localDateTime.atZone(ZoneId.systemDefault()));
            restriction.setCompleted(form.isCompleted());

            return restriction;

        }else{
            throw new RAObjectNotFoundException(form);
        }
    }

    @Override
    protected AccountRestrictionForm entityToForm(AccountRestriction entity) {
        return new AccountRestrictionForm(entity);
    }

    @Override
    protected AccountRestriction formToEntity(AccountRestrictionForm form) throws RAObjectNotFoundException {
        Account account = accountRepository.findById(form.getAccountId()).orElseThrow(()
                -> new RAObjectNotFoundException(Account.class, form.getAccountId()));
        AccountRestriction accountRestriction = new AccountRestriction();
        accountRestriction.setType(AccountRestrictionType.valueOf(form.getType()));
        accountRestriction.setAccount(account);
        accountRestriction.setAction(AccountRestrictionAction.valueOf(form.getAction()));
        accountRestriction.setDueBy(ZonedDateTime.from(AccountRestrictionForm.formatter.parse(form.getDueBy())));
        accountRestriction.setCompleted(form.isCompleted());
        return accountRestriction;
    }

    @Override
    protected AccountRestriction combine(AccountRestriction original, AccountRestriction updated) {
        //todo
        return null;
    }
}
