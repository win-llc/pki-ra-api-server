package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.*;
import com.winllc.pki.ra.beans.form.DomainLinkToAccountRequestForm;
import com.winllc.pki.ra.beans.info.AccountInfo;
import com.winllc.pki.ra.beans.info.DomainInfo;
import com.winllc.pki.ra.beans.info.DomainLinkToAccountRequestInfo;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.Domain;
import com.winllc.pki.ra.domain.DomainLinkToAccountRequest;
import com.winllc.pki.ra.domain.User;
import com.winllc.pki.ra.exception.NotAuthorizedException;
import com.winllc.pki.ra.exception.RAException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.DomainLinkToAccountRequestRepository;
import com.winllc.pki.ra.repository.DomainRepository;
import com.winllc.pki.ra.repository.UserRepository;
import com.winllc.pki.ra.security.RAUser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.validation.Valid;
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
    @ResponseStatus(HttpStatus.OK)
    public List<DomainLinkToAccountRequestInfo> getAllRequests(){
        List<DomainLinkToAccountRequest> requests = requestRepository.findAll();

        List<DomainLinkToAccountRequestInfo> infoList = requests.stream()
                .map(d -> buildInfo(d))
                .collect(Collectors.toList());

        return infoList;
    }

    @Transactional
    @GetMapping("/new")
    @ResponseStatus(HttpStatus.OK)
    public List<DomainLinkToAccountRequestInfo> getUnapprovedRequests(){
        List<DomainLinkToAccountRequest> requests = requestRepository.findAllByStatusEquals("new");

        List<DomainLinkToAccountRequestInfo> infoList = requests.stream()
                .map(d -> buildInfo(d))
                .collect(Collectors.toList());

        return infoList;
    }

    @GetMapping("/byId/{id}")
    @ResponseStatus(HttpStatus.OK)
    public DomainLinkToAccountRequest getById(@PathVariable Long id) throws RAObjectNotFoundException {
        Optional<DomainLinkToAccountRequest> requestOptional = requestRepository.findById(id);

        if(requestOptional.isPresent()){
            return requestOptional.get();
        }else{
            throw new RAObjectNotFoundException(DomainLinkToAccountRequest.class, id);
        }
    }

    @PostMapping("/linkAccount/create")
    @ResponseStatus(HttpStatus.CREATED)
    public Long createDomainRequest(@Valid @RequestBody DomainLinkToAccountRequestForm form,
                                    @AuthenticationPrincipal UserDetails raUser) throws NotAuthorizedException, RAObjectNotFoundException {
        DomainLinkToAccountRequest request = DomainLinkToAccountRequest.buildNew();

        List<Long> domainIds = new LinkedList<>(form.getRequestedDomainIds());
        domainIds.removeIf(Objects::isNull);
        form.setRequestedDomainIds(domainIds);

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
                return request.getId();
            }else{
                log.error("Requester not associated with account");
                throw new NotAuthorizedException(requester, "Link Account Create");
            }

        }else{
            if(!optionalAccount.isPresent()){
                throw new RAObjectNotFoundException(Account.class, form.getAccountId());
            }else{
                throw new RAObjectNotFoundException(User.class, raUser.getUsername());
            }
        }
    }

    @Transactional
    @PostMapping("/linkAccount/update")
    @ResponseStatus(HttpStatus.OK)
    public DomainLinkToAccountRequest domainRequestDecision(@Valid @RequestBody DomainLinkRequestDecision decision) throws RAException {
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
                    throw new RAObjectNotFoundException(Account.class, request.getAccountId());
                }
            }else if(decision.getStatus().contentEquals("reject")){
                request.setStatusRejected();
            }else{
                throw new RAException("Status not valid: "+decision.getStatus());
            }

            //workaround for element collection
            Set<Long> domainIds = new HashSet<>(request.getRequestedDomainIds());
            request.setRequestedDomainIds(domainIds);
            request = requestRepository.save(request);

            log.debug("Request updated: " + request);

            return request;
        }else{
            throw new RAObjectNotFoundException(DomainLinkToAccountRequest.class, decision.getRequestId());
        }
    }

    private DomainLinkToAccountRequestInfo buildInfo(DomainLinkToAccountRequest request){
        DomainLinkToAccountRequestInfo info = new DomainLinkToAccountRequestInfo(request);
        Optional<Account> optionalAccount = accountRepository.findById(request.getAccountId());
        List<Domain> domains = domainRepository.findAllByIdIn(request.getRequestedDomainIds());

        if(optionalAccount.isPresent()){
            AccountInfo accountInfo = new AccountInfo(optionalAccount.get(), false);
            info.setAccountInfo(accountInfo);
        }

        List<DomainInfo> domainInfoList = domains.stream()
                .map(d -> new DomainInfo(d, true))
                .collect(Collectors.toList());

        info.setDomainInfoList(domainInfoList);
        return info;
    }

}
