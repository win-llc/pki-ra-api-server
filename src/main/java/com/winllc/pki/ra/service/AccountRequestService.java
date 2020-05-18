package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.form.AccountRequestForm;
import com.winllc.pki.ra.beans.form.AccountRequestUpdateForm;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.AccountRequest;
import com.winllc.pki.ra.domain.User;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.AccountRequestRepository;
import com.winllc.pki.ra.repository.UserRepository;
import com.winllc.pki.ra.security.RAUser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/account/request")
public class AccountRequestService {

    private static final Logger log = LogManager.getLogger(AccountRequestService.class);

    @Autowired
    private AccountRequestRepository accountRequestRepository;
    @Autowired
    private AccountService accountService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private UserRepository userRepository;

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


    @PostMapping("/submit")
    @ResponseStatus(HttpStatus.CREATED)
    public Long createAccountRequest(@Valid @RequestBody AccountRequestForm form) throws RAObjectNotFoundException{
        Optional<User> userOptional = userRepository.findOneByUsername(form.getAccountOwnerEmail());
        if(userOptional.isPresent()){
            User user = userOptional.get();

            AccountRequest accountRequest = AccountRequest.createNew();
            accountRequest.setAccountOwner(user);
            accountRequest.setProjectName(form.getProjectName());

            accountRequest = accountRequestRepository.save(accountRequest);
            return accountRequest.getId();
        }else{
            throw new RAObjectNotFoundException(User.class, form.getAccountOwnerEmail());
        }
    }

    @PostMapping("/update")
    @Transactional
    public ResponseEntity<?> accountRequestUpdate(@Valid @RequestBody AccountRequestUpdateForm form) throws RAObjectNotFoundException {
        Optional<AccountRequest> optionalAccountRequest = accountRequestRepository.findById(form.getAccountRequestId());

        if(optionalAccountRequest.isPresent()){
            AccountRequest accountRequest = optionalAccountRequest.get();
            if(form.getState().contentEquals("approve")){
                accountRequest.approve();

                Account account = accountService.buildNew();
                account.setProjectName(accountRequest.getProjectName());

                User accountOwner = accountRequest.getAccountOwner();

                account.getAccountUsers().add(accountOwner);
                account = accountRepository.save(account);

                accountOwner.getAccounts().add(account);

                userRepository.save(accountOwner);
            }else if(form.getState().contentEquals("reject")){
                accountRequest.reject();
            }else{
                return ResponseEntity.badRequest().build();
            }

            accountRequest = accountRequestRepository.save(accountRequest);

            return ResponseEntity.ok(accountRequest);
        }else{
            throw new RAObjectNotFoundException(form);
        }
    }

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

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id){

        accountRequestRepository.deleteById(id);

        return ResponseEntity.ok().build();
    }
}
