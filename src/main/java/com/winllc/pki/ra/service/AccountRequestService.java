package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.AccountRequestForm;
import com.winllc.pki.ra.beans.AccountRequestUpdateForm;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.AccountRequest;
import com.winllc.pki.ra.domain.User;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.AccountRequestRepository;
import com.winllc.pki.ra.repository.UserRepository;
import com.winllc.pki.ra.security.RAUser;
import com.winllc.pki.ra.util.AppUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.Base64;
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
    public ResponseEntity<?> findAll(){
        List<AccountRequest> accountRequests = accountRequestRepository.findAll();
        return ResponseEntity.ok(accountRequests);
    }

    @GetMapping("/pending")
    public ResponseEntity<?> findPending(){
        List<AccountRequest> accountRequests = accountRequestRepository.findAllByStateEquals("new");
        return ResponseEntity.ok(accountRequests);
    }


    @PostMapping("/submit")
    public ResponseEntity<?> createAccountRequest(@RequestBody AccountRequestForm form, @AuthenticationPrincipal RAUser raUser){
        Optional<User> userOptional = userRepository.findOneByUsername(form.getAccountOwnerEmail());
        if(userOptional.isPresent()){
            User user = userOptional.get();

            AccountRequest accountRequest = AccountRequest.createNew();
            accountRequest.setAccountOwner(user);
            accountRequest.setProjectName(form.getProjectName());

            accountRequest = accountRequestRepository.save(accountRequest);
            return ResponseEntity.ok(accountRequest);
        }else{
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/update")
    @Transactional
    public ResponseEntity<?> accountRequestUpdate(@RequestBody AccountRequestUpdateForm form){
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
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/findById/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id){
        Optional<AccountRequest> optionalAccountRequest = accountRequestRepository.findById(id);

        if(optionalAccountRequest.isPresent()){
            return ResponseEntity.ok(optionalAccountRequest.get());
        }else{
            return ResponseEntity.notFound().build();
        }
    }
}
