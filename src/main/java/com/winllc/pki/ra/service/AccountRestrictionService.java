package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.form.AccountRestrictionForm;
import com.winllc.pki.ra.constants.AccountRestrictionAction;
import com.winllc.pki.ra.constants.AccountRestrictionType;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.AccountRestriction;
import com.winllc.pki.ra.domain.Domain;
import com.winllc.pki.ra.domain.Notification;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.AccountRestrictionRepository;
import com.winllc.pki.ra.service.transaction.SystemActionRunner;
import com.winllc.pki.ra.service.transaction.ThrowingSupplier;
import com.winllc.pki.ra.service.validators.AccountRestrictionValidator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accountRestriction")
public class AccountRestrictionService extends AbstractService {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    private static final Logger log = LogManager.getLogger(AccountRestrictionService.class);

    private final AccountRepository accountRepository;
    private final AccountRestrictionRepository accountRestrictionRepository;
    private final AccountRestrictionValidator accountRestrictionValidator;

    public AccountRestrictionService(AccountRepository accountRepository,
                                     AccountRestrictionRepository accountRestrictionRepository,
                                     AccountRestrictionValidator accountRestrictionValidator,
                                     ApplicationContext applicationContext) {
        super(applicationContext);
        this.accountRepository = accountRepository;
        this.accountRestrictionRepository = accountRestrictionRepository;
        this.accountRestrictionValidator = accountRestrictionValidator;
    }

    @InitBinder("accountRestrictionForm")
    public void initAccountRequestUpdateBinder(WebDataBinder binder) {
        binder.setValidator(accountRestrictionValidator);
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

    @PreAuthorize("hasPermission(#id, 'com.winllc.pki.ra.domain.AccountRestriction', 'view_account_restriction')")
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

        SystemActionRunner runner = SystemActionRunner.build(this.context);
        runner.createNotification(notification)
                .markEntityAsTask(accountRestriction.getDueBy().toLocalDateTime());

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

    @PreAuthorize("hasPermission(#id, 'com.winllc.pki.ra.domain.AccountRestriction', 'delete_account_restriction')")
    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void delete(@PathVariable Long id){
        accountRestrictionRepository.deleteById(id);
    }

    @PreAuthorize("hasPermission(#accountId, 'com.winllc.pki.ra.domain.AccountRestriction', 'view_account_restriction')")
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
            restriction.setDueBy(Timestamp.valueOf(localDateTime));
            restriction.setCompleted(form.isCompleted());

            return restriction;

        }else{
            throw new RAObjectNotFoundException(form);
        }
    }
}
