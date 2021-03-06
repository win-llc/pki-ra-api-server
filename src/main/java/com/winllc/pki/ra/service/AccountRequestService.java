package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.form.AccountRequestForm;
import com.winllc.pki.ra.beans.form.AccountRequestUpdateForm;
import com.winllc.acme.common.domain.AccountRequest;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.AccountRequestRepository;
import com.winllc.pki.ra.service.validators.AccountRequestUpdateValidator;
import com.winllc.pki.ra.service.validators.AccountRequestValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/account/request")
public class AccountRequestService {

    private static final Logger log = LogManager.getLogger(AccountRequestService.class);

    private final AccountRequestRepository accountRequestRepository;
    private final AccountService accountService;

    private final AccountRequestValidator accountRequestValidator;
    private final AccountRequestUpdateValidator accountRequestUpdateValidator;

    public AccountRequestService(AccountRequestRepository accountRequestRepository,
                                 AccountRequestValidator accountRequestValidator,
                                 AccountRequestUpdateValidator accountRequestUpdateValidator, AccountService accountService) {
        this.accountRequestRepository = accountRequestRepository;
        this.accountRequestValidator = accountRequestValidator;
        this.accountRequestUpdateValidator = accountRequestUpdateValidator;
        this.accountService = accountService;
    }

    @InitBinder("accountRequestForm")
    public void initAccountRequestBinder(WebDataBinder binder) {
        binder.setValidator(accountRequestValidator);
    }

    @InitBinder("accountRequestUpdateForm")
    public void initAccountRequestUpdateBinder(WebDataBinder binder) {
        binder.setValidator(accountRequestUpdateValidator);
    }

    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public List<AccountRequest> findAll(){
        List<AccountRequest> accountRequests = accountRequestRepository.findAll();
        return accountRequests;
    }

    @GetMapping("/pending")
    @ResponseStatus(HttpStatus.OK)
    public List<AccountRequest> findPending(){
        List<AccountRequest> accountRequests = accountRequestRepository.findAllByStateEquals("new");
        return accountRequests;
    }

    @GetMapping("/pending/count")
    @ResponseStatus(HttpStatus.OK)
    public Integer findPendingCount(){
        return accountRequestRepository.countAllByStateEquals("new");
    }

    @GetMapping("/my")
    @ResponseStatus(HttpStatus.OK)
    public List<AccountRequest> myRequests(Authentication authentication){
        return accountRequestRepository.findAllByRequestedByEmailEquals(authentication.getName());
    }

    //@PreAuthorize("hasPermission(#form, 'accountrequest:create')")
    @PostMapping("/submit")
    @ResponseStatus(HttpStatus.CREATED)
    public Long createAccountRequest(@Valid @RequestBody AccountRequestForm form, Authentication authentication) {
        log.info("Account request: "+form);
        AccountRequest accountRequest = AccountRequest.createNew();
        accountRequest.setAccountOwnerEmail(form.getAccountOwnerEmail());
        accountRequest.setProjectName(form.getProjectName());
        accountRequest.setSecurityPolicyServerProjectId(form.getSecurityPolicyServerProjectId());
        accountRequest.setRequestedByEmail(authentication.getName());
        accountRequest.setCreationDate(Timestamp.from(ZonedDateTime.now().toInstant()));

        accountRequest = accountRequestRepository.save(accountRequest);
        return accountRequest.getId();
    }

    //@PreAuthorize("hasPermission(#form, 'accountrequest:update')")
    @PostMapping("/update")
    @Transactional
    public ResponseEntity<?> accountRequestUpdate(@Valid @RequestBody AccountRequestUpdateForm form) throws Exception {
        Optional<AccountRequest> optionalAccountRequest = accountRequestRepository.findById(form.getAccountRequestId());

        if(optionalAccountRequest.isPresent()){
            AccountRequest accountRequest = optionalAccountRequest.get();
            if(form.getState().contentEquals("approve")){
                accountRequest.approve();

                AccountRequestForm requestForm = new AccountRequestForm();
                requestForm.setProjectName(accountRequest.getProjectName());
                requestForm.setAccountOwnerEmail(accountRequest.getAccountOwnerEmail());
                requestForm.setSecurityPolicyServerProjectId(accountRequest.getSecurityPolicyServerProjectId());

                accountService.createNewAccount(requestForm);
            }else if(form.getState().contentEquals("reject")){
                accountRequest.reject();
            }

            accountRequest = accountRequestRepository.save(accountRequest);

            return ResponseEntity.ok(accountRequest);
        }else{
            throw new RAObjectNotFoundException(form);
        }
    }

    //@PreAuthorize("hasPermission(#id, 'com.winllc.acme.common.domain.AccountRequest', 'accountrequest:read')")
    @GetMapping("/findById/{id}")
    @ResponseStatus(HttpStatus.OK)
    public AccountRequest findById(@PathVariable Long id) throws RAObjectNotFoundException {
        Optional<AccountRequest> optionalAccountRequest = accountRequestRepository.findById(id);

        if(optionalAccountRequest.isPresent()){
            return optionalAccountRequest.get();
        }else{
            throw new RAObjectNotFoundException(AccountRequest.class, id);
        }
    }

    //@PreAuthorize("hasPermission(#id, 'com.winllc.acme.common.domain.AccountRequest', 'accountrequest:delete')")
    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void delete(@PathVariable Long id){

        accountRequestRepository.deleteById(id);
    }
}
