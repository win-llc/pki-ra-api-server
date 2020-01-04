package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.DomainLinkRequestDecision;
import com.winllc.pki.ra.beans.DomainLinkToAccountRequestForm;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.Domain;
import com.winllc.pki.ra.domain.DomainLinkToAccountRequest;
import com.winllc.pki.ra.domain.User;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.DomainLinkToAccountRequestRepository;
import com.winllc.pki.ra.repository.DomainRepository;
import com.winllc.pki.ra.repository.UserRepository;
import com.winllc.pki.ra.security.RAUser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.websocket.server.PathParam;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/domain/request")
public class DomainLinkToAccountRequestService {

    private static final Logger log = LogManager.getLogger(DomainLinkToAccountRequestService.class);

    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private DomainLinkToAccountRequestRepository requestRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private UserRepository userRepository;

    @Transactional
    @GetMapping("/all")
    public ResponseEntity<?> getAllRequests(){
        List<DomainLinkToAccountRequest> requests = requestRepository.findAll();

        return ResponseEntity.ok(requests);
    }

    @Transactional
    @GetMapping("/new")
    public ResponseEntity<?> getUnapprovedRequests(){
        List<DomainLinkToAccountRequest> requests = requestRepository.findAllByStatusEquals("new");

        return ResponseEntity.ok(requests);
    }

    @GetMapping("/byId/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id){
        Optional<DomainLinkToAccountRequest> requestOptional = requestRepository.findById(id);

        if(requestOptional.isPresent()){
            return ResponseEntity.ok(requestOptional.get());
        }else{
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/linkAccount/create")
    public ResponseEntity<?> createDomainRequest(@RequestBody DomainLinkToAccountRequestForm form, @AuthenticationPrincipal RAUser raUser){
        DomainLinkToAccountRequest request = DomainLinkToAccountRequest.buildNew();

        form.getRequestedDomainIds().removeIf(Objects::isNull);

        Optional<User> optionalUser = userRepository.findOneByUsername(raUser.getUsername());
        Optional<Account> optionalAccount = accountRepository.findById(form.getAccountId());
        List<Domain> requestedDomains = domainRepository.findAllByIdIn(form.getRequestedDomainIds());
        if(optionalUser.isPresent() && optionalAccount.isPresent()){
            User requester = optionalUser.get();
            Account account = optionalAccount.get();

            //Ensure user exists in the account

            List<User> accountUsers = userRepository.findAllByAccountsContains(account);
            if(accountUsers.contains(requester)){
                Set<Long> avaialableDomains = requestedDomains.stream()
                        .map(r -> r.getId())
                        .collect(Collectors.toSet());
                request.setRequestedDomainIds(avaialableDomains);
                request.setAccountId(account.getId());

                request = requestRepository.save(request);
                log.info("Created domain link request: "+request);
                return ResponseEntity.ok().build();
            }else{
                log.error("Requester not associated with account");
                return ResponseEntity.status(401).build();
            }

        }else{
            return ResponseEntity.badRequest().build();
        }
    }

    @Transactional
    @PostMapping("/linkAccount/update")
    public ResponseEntity<?> domainRequestDecision(@RequestBody DomainLinkRequestDecision decision) {
        Optional<DomainLinkToAccountRequest> optionalDomainLinkToAccountRequest = requestRepository.findById(decision.getRequestId());
        if(optionalDomainLinkToAccountRequest.isPresent()){
            DomainLinkToAccountRequest request = optionalDomainLinkToAccountRequest.get();
            if(decision.getStatus().contentEquals("approve")){
                request.setStatusApproved();

                Optional<Account> optionalAccount = accountRepository.findById(request.getAccountId());
                List<Domain> requestedDomains = domainRepository.findAllByIdIn(new ArrayList(request.getRequestedDomainIds()));
                if(optionalAccount.isPresent()){
                    Account account = optionalAccount.get();

                    account.getCanIssueDomains().addAll(requestedDomains);

                    account = accountRepository.save(account);

                    for(Domain requestedDomain : requestedDomains){
                        requestedDomain.getCanIssueAccounts().add(account);
                        domainRepository.save(requestedDomain);
                    }
                }else{
                    log.error("Could not find requested account");
                    return ResponseEntity.badRequest().build();
                }
            }else if(decision.getStatus().contentEquals("reject")){
                request.setStatusRejected();

            }else{
                return ResponseEntity.badRequest().build();
            }

            request = requestRepository.save(request);

            log.debug("Request updated: " + request);

            return ResponseEntity.ok().build();

        }else{
            return ResponseEntity.badRequest().build();
        }
    }

}
