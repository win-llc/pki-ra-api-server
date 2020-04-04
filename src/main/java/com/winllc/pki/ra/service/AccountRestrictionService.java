package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.form.AccountRestrictionForm;
import com.winllc.pki.ra.constants.AccountRestrictionAction;
import com.winllc.pki.ra.constants.AccountRestrictionType;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.AccountRestriction;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.AccountRestrictionRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accountRestriction")
public class AccountRestrictionService {

    private DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    private static final Logger log = LogManager.getLogger(AccountRestrictionService.class);

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountRestrictionRepository accountRestrictionRepository;

    @GetMapping("/types")
    public ResponseEntity<?> getRestrictionTypes(){
        return ResponseEntity.ok(AccountRestrictionType.values());
    }

    @GetMapping("/actions")
    public ResponseEntity<?> getRestrictionActions(){
        return ResponseEntity.ok(AccountRestrictionAction.values());
    }

    @GetMapping("/byId/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) throws RAObjectNotFoundException {
        Optional<AccountRestriction> optionalAccountRestriction = accountRestrictionRepository.findById(id);
        if(optionalAccountRestriction.isPresent()){
            AccountRestriction restriction = optionalAccountRestriction.get();

            return ResponseEntity.ok(new AccountRestrictionForm(restriction));
        }else{
            throw new RAObjectNotFoundException(AccountRestriction.class, id);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@Valid @RequestBody AccountRestrictionForm form) throws Exception {
        AccountRestriction accountRestriction = formToRestriction(form);

        accountRestriction = accountRestrictionRepository.save(accountRestriction);

        log.info("Account restriction added ID: "+accountRestriction.getId());

        return ResponseEntity.ok(accountRestriction.getId());
    }

    @PreAuthorize("hasPermission(#form, 'update_account_restriction')")
    @PostMapping("/update")
    public ResponseEntity<?> update(@Valid @RequestBody AccountRestrictionForm form) throws Exception {

        Optional<AccountRestriction> optionalAccountRestriction = accountRestrictionRepository.findById(form.getId());

        if(optionalAccountRestriction.isPresent()){
            AccountRestriction existing = optionalAccountRestriction.get();
            form.setAccountId(existing.getAccount().getId());
            AccountRestriction fromForm = formToRestriction(form);

            existing.setDueBy(fromForm.getDueBy());
            existing.setCompleted(form.isCompleted());

            existing = accountRestrictionRepository.save(existing);

            return ResponseEntity.ok(existing);
        }else{
            throw new RAObjectNotFoundException(form);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id){

        accountRestrictionRepository.deleteById(id);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/allForAccount/{accountId}")
    public ResponseEntity<?> getAllForAccount(@PathVariable Long accountId){
        Optional<Account> optionalAccount = accountRepository.findById(accountId);
        if(optionalAccount.isPresent()){
            Account account = optionalAccount.get();

            List<AccountRestriction> allByAccount = accountRestrictionRepository.findAllByAccount(account);

            return ResponseEntity.ok(allByAccount.stream()
            .map(AccountRestrictionForm::new)
            .collect(Collectors.toList()));
        }else{
            return ResponseEntity.badRequest().build();
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