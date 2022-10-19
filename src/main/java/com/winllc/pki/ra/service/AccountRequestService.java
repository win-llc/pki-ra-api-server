package com.winllc.pki.ra.service;

import com.winllc.acme.common.domain.TermsOfService;
import com.winllc.acme.common.repository.TermsOfServiceRepository;
import com.winllc.pki.ra.beans.form.AccountRequestForm;
import com.winllc.pki.ra.beans.form.AccountRequestUpdateForm;
import com.winllc.acme.common.domain.AccountRequest;
import com.winllc.pki.ra.beans.form.AccountUpdateForm;
import com.winllc.pki.ra.beans.form.TermsOfServiceForm;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.AccountRequestRepository;
import com.winllc.pki.ra.service.validators.AccountRequestUpdateValidator;
import com.winllc.pki.ra.service.validators.AccountRequestValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import javax.validation.Valid;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/account/request")
public class AccountRequestService extends
        DataPagedService<AccountRequest, AccountRequestForm,
                AccountRequestRepository> {

    private static final Logger log = LogManager.getLogger(AccountRequestService.class);

    private final AccountRequestRepository accountRequestRepository;
    private final AccountService accountService;

    private final AccountRequestValidator accountRequestValidator;
    private final AccountRequestUpdateValidator accountRequestUpdateValidator;

    public AccountRequestService(ApplicationContext context,
                                 AccountRequestRepository accountRequestRepository,
                                 AccountRequestValidator accountRequestValidator,
                                 AccountRequestUpdateValidator accountRequestUpdateValidator, AccountService accountService) {
        super(context, AccountRequest.class, accountRequestRepository);
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
    public List<AccountRequest> findAll() {
        List<AccountRequest> accountRequests = accountRequestRepository.findAll();
        return accountRequests;
    }

    @GetMapping("/pending")
    @ResponseStatus(HttpStatus.OK)
    public List<AccountRequest> findPending() {
        List<AccountRequest> accountRequests = accountRequestRepository.findAllByStateEquals("new");
        return accountRequests;
    }

    @GetMapping("/pending/count")
    @ResponseStatus(HttpStatus.OK)
    public Integer findPendingCount() {
        return accountRequestRepository.countAllByStateEquals("new");
    }

    @GetMapping("/my")
    @ResponseStatus(HttpStatus.OK)
    public List<AccountRequest> myRequests(Authentication authentication) {
        return accountRequestRepository.findAllByRequestedByEmailEquals(authentication.getName());
    }

    //@PreAuthorize("hasPermission(#form, 'accountrequest:create')")
    @PostMapping("/submit")
    @ResponseStatus(HttpStatus.CREATED)
    public Long createAccountRequest(@Valid @RequestBody AccountRequestForm form, Authentication authentication) {
        log.info("Account request: " + form);
        AccountRequest accountRequest = AccountRequest.createNew();
        accountRequest.setAccountOwnerEmail(form.getAccountOwnerEmail());
        accountRequest.setProjectName(form.getProjectName());
        accountRequest.setSecurityPolicyServerProjectId(form.getSecurityPolicyServerProjectId());
        accountRequest.setRequestedByEmail(authentication.getName());
        accountRequest.setCreationDate(Timestamp.from(ZonedDateTime.now().toInstant()));

        accountRequest = accountRequestRepository.save(accountRequest);
        return accountRequest.getId();
    }


    //@PreAuthorize("hasPermission(#id, 'com.winllc.acme.common.domain.AccountRequest', 'accountrequest:read')")
    @GetMapping("/findById/{id}")
    @ResponseStatus(HttpStatus.OK)
    public AccountRequest findById(@PathVariable Long id) throws RAObjectNotFoundException {
        Optional<AccountRequest> optionalAccountRequest = accountRequestRepository.findById(id);

        if (optionalAccountRequest.isPresent()) {
            return optionalAccountRequest.get();
        } else {
            throw new RAObjectNotFoundException(AccountRequest.class, id);
        }
    }


    @Override
    protected AccountRequestForm entityToForm(AccountRequest entity) {
        return new AccountRequestForm(entity);
    }

    @Override
    protected AccountRequest formToEntity(AccountRequestForm form, Authentication authentication) throws Exception {
        AccountRequest accountRequest = AccountRequest.createNew();
        accountRequest.setAccountOwnerEmail(form.getAccountOwnerEmail());
        accountRequest.setProjectName(form.getProjectName());
        accountRequest.setSecurityPolicyServerProjectId(form.getSecurityPolicyServerProjectId());
        accountRequest.setRequestedByEmail(authentication.getName());
        accountRequest.setCreationDate(Timestamp.from(ZonedDateTime.now().toInstant()));
        accountRequest.setState(form.getState());

        return accountRequest;
    }

    @Override
    protected AccountRequest combine(AccountRequest original, AccountRequest updated,
                                     Authentication authentication) throws Exception {
        if (updated.getState().contentEquals("approve")) {
            original.approve();

            AccountUpdateForm requestForm = new AccountUpdateForm();
            requestForm.setProjectName(original.getProjectName());
            requestForm.setAccountOwnerEmail(original.getAccountOwnerEmail());
            requestForm.setSecurityPolicyServerProjectId(original.getSecurityPolicyServerProjectId());

            accountService.add(requestForm, authentication);
        } else if (updated.getState().contentEquals("reject")) {
            original.reject();
        }

        return original;
    }

    @Override
    public List<Predicate> buildFilter(Map<String, String> allRequestParams, Root<AccountRequest> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        return null;
    }
}
