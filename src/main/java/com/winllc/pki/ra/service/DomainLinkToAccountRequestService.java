package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.form.DomainLinkRequestDecisionForm;
import com.winllc.pki.ra.beans.form.DomainLinkToAccountRequestForm;
import com.winllc.pki.ra.beans.info.AccountInfo;
import com.winllc.pki.ra.beans.info.DomainInfo;
import com.winllc.pki.ra.beans.info.DomainLinkToAccountRequestInfo;
import com.winllc.pki.ra.constants.AuditRecordType;
import com.winllc.pki.ra.domain.*;
import com.winllc.pki.ra.exception.NotAuthorizedException;
import com.winllc.pki.ra.exception.RAException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.*;
import com.winllc.pki.ra.service.transaction.SystemActionRunner;
import com.winllc.pki.ra.service.validators.DomainLinkRequestDecisionValidator;
import com.winllc.pki.ra.service.validators.DomainLinkToAccountRequestValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/domain/request")
public class DomainLinkToAccountRequestService extends AbstractService {

    private static final Logger log = LogManager.getLogger(DomainLinkToAccountRequestService.class);

    private final DomainRepository domainRepository;
    private final DomainLinkToAccountRequestRepository requestRepository;
    private final AccountRepository accountRepository;
    private final PocEntryRepository pocEntryRepository;
    private final DomainPolicyRepository domainPolicyRepository;
    private final DomainLinkToAccountRequestValidator domainLinkToAccountRequestValidator;
    private final DomainLinkRequestDecisionValidator domainLinkRequestDecisionValidator;

    public DomainLinkToAccountRequestService(ApplicationContext context, DomainRepository domainRepository,
                                             DomainLinkToAccountRequestRepository requestRepository,
                                             AccountRepository accountRepository, PocEntryRepository pocEntryRepository,
                                             DomainPolicyRepository domainPolicyRepository,
                                             DomainLinkToAccountRequestValidator domainLinkToAccountRequestValidator, DomainLinkRequestDecisionValidator domainLinkRequestDecisionValidator) {
        super(context);
        this.domainRepository = domainRepository;
        this.requestRepository = requestRepository;
        this.accountRepository = accountRepository;
        this.pocEntryRepository = pocEntryRepository;
        this.domainPolicyRepository = domainPolicyRepository;
        this.domainLinkToAccountRequestValidator = domainLinkToAccountRequestValidator;
        this.domainLinkRequestDecisionValidator = domainLinkRequestDecisionValidator;
    }

    @InitBinder("domainLinkToAccountRequestForm")
    public void initBinder(WebDataBinder binder) {
        binder.setValidator(domainLinkToAccountRequestValidator);
    }

    @InitBinder("domainLinkRequestDecisionForm")
    public void initDecisionBinder(WebDataBinder binder) {
        binder.setValidator(domainLinkRequestDecisionValidator);
    }

    @Transactional
    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public List<DomainLinkToAccountRequestInfo> getAllRequests() {
        List<DomainLinkToAccountRequest> requests = requestRepository.findAll();

        List<DomainLinkToAccountRequestInfo> infoList = requests.stream()
                .map(d -> buildInfo(d))
                .collect(Collectors.toList());

        return infoList;
    }

    @Transactional
    @GetMapping("/new")
    @ResponseStatus(HttpStatus.OK)
    public List<DomainLinkToAccountRequestInfo> getUnapprovedRequests() {
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

        if (requestOptional.isPresent()) {
            return requestOptional.get();
        } else {
            throw new RAObjectNotFoundException(DomainLinkToAccountRequest.class, id);
        }
    }

    @PostMapping("/linkAccount/create")
    @ResponseStatus(HttpStatus.CREATED)
    public Long createDomainRequest(@Valid @RequestBody DomainLinkToAccountRequestForm form,
                                    @AuthenticationPrincipal UserDetails raUser)
            throws NotAuthorizedException, RAObjectNotFoundException {

        DomainLinkToAccountRequest request = DomainLinkToAccountRequest.buildNew();

        List<Long> domainIds = new LinkedList<>(form.getRequestedDomainIds());
        domainIds.removeIf(Objects::isNull);
        form.setRequestedDomainIds(domainIds);

        Optional<Account> optionalAccount = accountRepository.findById(form.getAccountId());
        List<Domain> requestedDomains = domainRepository.findAllByIdIn(form.getRequestedDomainIds());
        if (optionalAccount.isPresent()) {
            Account account = optionalAccount.get();

            //Ensure user exists in the account
            Optional<PocEntry> pocEntryOptional = pocEntryRepository
                    .findDistinctByEmailEqualsAndAccount(raUser.getUsername(), account);

            //List<User> accountUsers = userRepository.findAllByAccountsContains(account);
            if (pocEntryOptional.isPresent()) {
                Set<Long> avaialableDomains = requestedDomains.stream()
                        .map(r -> r.getId())
                        .collect(Collectors.toSet());
                request.setRequestedDomainIds(avaialableDomains);
                request.setAccountId(account.getId());

                request = requestRepository.save(request);

                SystemActionRunner.build(context)
                        .createNotificationForAccountPocs(Notification.buildNew()
                                .addMessage("Domain Link to Account Requested"), account)
                        .createAuditRecord(AuditRecordType.DOMAIN_LINK_TO_ACCOUNT_REQUEST_CREATED)
                        .execute();

                log.info("Created domain link request: " + request);
                return request.getId();
            } else {
                log.error("Requester not associated with account");
                throw new NotAuthorizedException(raUser.getUsername(), "Link Account Create");
            }

        } else {
            throw new RAObjectNotFoundException(Account.class, form.getAccountId());
        }
    }

    @Transactional
    @PostMapping("/linkAccount/update")
    @ResponseStatus(HttpStatus.OK)
    public DomainLinkToAccountRequest domainRequestDecision(@Valid @RequestBody DomainLinkRequestDecisionForm decision) throws RAException {
        Optional<DomainLinkToAccountRequest> optionalDomainLinkToAccountRequest = requestRepository.findById(decision.getRequestId());
        if (optionalDomainLinkToAccountRequest.isPresent()) {
            DomainLinkToAccountRequest request = optionalDomainLinkToAccountRequest.get();

            Optional<Account> optionalAccount = accountRepository.findById(request.getAccountId());
            if (optionalAccount.isPresent()) {
                Account account = optionalAccount.get();

                switch (decision.getStatus()) {
                    case "approve":
                        request.setStatusApproved();

                        List<Domain> requestedDomains = domainRepository.findAllByIdIn(new ArrayList<>(request.getRequestedDomainIds()));

                        if (!CollectionUtils.isEmpty(requestedDomains)) {
                            Set<DomainPolicy> domainPolicies = requestedDomains.stream()
                                    .map(DomainPolicy::new)
                                    .collect(Collectors.toSet());

                            Set<DomainPolicy> saved = new HashSet<>();
                            for (DomainPolicy dp : domainPolicies) {
                                DomainPolicy temp = domainPolicyRepository.save(dp);
                                Domain domain = dp.getTargetDomain();

                                domain.getAllDomainPolicies().add(temp);
                                domainRepository.save(domain);

                                saved.add(temp);
                            }

                            account.getAccountDomainPolicies().addAll(saved);
                            accountRepository.save(account);

                            SystemActionRunner.build(context)
                                    .createNotificationForAccountPocs(Notification.buildNew()
                                            .addMessage("Domain Link to Account Approved"), account)
                                    .createAuditRecord(AuditRecordType.DOMAIN_LINK_TO_ACCOUNT_REQUEST_APPROVED)
                                    .execute();
                        }

                        break;
                    case "reject":
                        request.setStatusRejected();

                        SystemActionRunner.build(context)
                                .createNotificationForAccountPocs(Notification.buildNew()
                                        .addMessage("Domain Link to Account Approved"), account)
                                .createAuditRecord(AuditRecordType.DOMAIN_LINK_TO_ACCOUNT_REQUEST_APPROVED)
                                .execute();

                        break;
                }
            }

            //workaround for element collection
            Set<Long> domainIds = new HashSet<>(request.getRequestedDomainIds());
            request.setRequestedDomainIds(domainIds);
            request = requestRepository.save(request);

            log.debug("Request updated: " + request);

            return request;
        } else {
            throw new RAObjectNotFoundException(DomainLinkToAccountRequest.class, decision.getRequestId());
        }
    }

    private DomainLinkToAccountRequestInfo buildInfo(DomainLinkToAccountRequest request) {
        DomainLinkToAccountRequestInfo info = new DomainLinkToAccountRequestInfo(request);
        Optional<Account> optionalAccount = accountRepository.findById(request.getAccountId());
        List<Domain> domains = domainRepository.findAllByIdIn(request.getRequestedDomainIds());

        if (optionalAccount.isPresent()) {
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
