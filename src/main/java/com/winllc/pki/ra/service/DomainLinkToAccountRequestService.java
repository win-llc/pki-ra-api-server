package com.winllc.pki.ra.service;

import com.winllc.acme.common.domain.Account;
import com.winllc.pki.ra.beans.form.AccountRequestForm;
import com.winllc.pki.ra.beans.form.DomainLinkRequestDecisionForm;
import com.winllc.pki.ra.beans.form.DomainLinkToAccountRequestForm;
import com.winllc.pki.ra.beans.info.AccountInfo;
import com.winllc.pki.ra.beans.info.DomainInfo;
import com.winllc.pki.ra.beans.info.DomainLinkToAccountRequestInfo;
import com.winllc.acme.common.constants.AuditRecordType;
import com.winllc.acme.common.domain.*;
import com.winllc.pki.ra.exception.NotAuthorizedException;
import com.winllc.pki.ra.exception.RAException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.*;
import com.winllc.pki.ra.service.transaction.SystemActionRunner;
import com.winllc.pki.ra.service.validators.DomainLinkRequestDecisionValidator;
import com.winllc.pki.ra.service.validators.DomainLinkToAccountRequestValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import javax.validation.Valid;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/domain/request")
public class DomainLinkToAccountRequestService extends
        DataPagedService<DomainLinkToAccountRequest, DomainLinkToAccountRequestForm,
                DomainLinkToAccountRequestRepository> {

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
        super(context, DomainLinkToAccountRequest.class, requestRepository);
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
    @GetMapping("/new")
    @ResponseStatus(HttpStatus.OK)
    public List<DomainLinkToAccountRequestInfo> getUnapprovedRequests() {
        List<DomainLinkToAccountRequest> requests = requestRepository.findAllByStatusEquals("new");

        List<DomainLinkToAccountRequestInfo> infoList = requests.stream()
                .map(d -> buildInfo(d))
                .collect(Collectors.toList());

        return infoList;
    }

    @GetMapping("/new/count")
    @ResponseStatus(HttpStatus.OK)
    public Integer findByStatusCount() {
        return requestRepository.countAllByStatusEquals("new");
    }

    @Transactional
    @GetMapping("/my")
    @ResponseStatus(HttpStatus.OK)
    public List<DomainLinkToAccountRequestInfo> getMyRequests(Authentication authentication) {

        List<Account> userAccounts = accountRepository.findAllByPocsEmailEquals(authentication.getName());
        List<DomainLinkToAccountRequest> requests = requestRepository.findAllByAccountIdIn(userAccounts.stream().map(a -> a.getId()).collect(Collectors.toList()));

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

    @Transactional
    @GetMapping("/infoById/{id}")
    @ResponseStatus(HttpStatus.OK)
    public DomainLinkToAccountRequestInfo getInfoById(@PathVariable Long id) throws RAObjectNotFoundException {
        Optional<DomainLinkToAccountRequest> requestOptional = requestRepository.findById(id);

        if (requestOptional.isPresent()) {
            DomainLinkToAccountRequestInfo info = buildInfo(requestOptional.get());
            return info;
        } else {
            throw new RAObjectNotFoundException(DomainLinkToAccountRequest.class, id);
        }
    }

    @PostMapping("/linkAccount/create")
    @ResponseStatus(HttpStatus.CREATED)
    public Long createDomainRequest(@Valid @RequestBody DomainLinkToAccountRequestForm form,
                                    Authentication authentication)
            throws NotAuthorizedException, RAObjectNotFoundException {

        DomainLinkToAccountRequest request = DomainLinkToAccountRequest.buildNew();
        request.setRequestedBy(authentication.getName());
        request.setRequestedOn(ZonedDateTime.now());

        List<Long> domainIds = new LinkedList<>(form.getRequestedDomainIds());
        domainIds.removeIf(Objects::isNull);
        form.setRequestedDomainIds(domainIds);

        Optional<Account> optionalAccount = accountRepository.findById(form.getAccountId());
        List<Domain> requestedDomains = domainRepository.findAllByIdIn(form.getRequestedDomainIds());
        if (optionalAccount.isPresent()) {
            Account account = optionalAccount.get();

            //Ensure user exists in the account
            Optional<PocEntry> pocEntryOptional = pocEntryRepository
                    .findDistinctByEmailEqualsAndAccount(authentication.getName(), account);

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
                throw new NotAuthorizedException(authentication.getName(), "Link Account Create");
            }

        } else {
            throw new RAObjectNotFoundException(Account.class, form.getAccountId());
        }
    }

    @Transactional
    @PostMapping("/linkAccount/update")
    @ResponseStatus(HttpStatus.OK)
    public DomainLinkToAccountRequest domainRequestDecision(@Valid @RequestBody DomainLinkRequestDecisionForm decision,
                                                            Authentication authentication) throws RAException {
        Optional<DomainLinkToAccountRequest> optionalDomainLinkToAccountRequest = requestRepository.findById(decision.getRequestId());
        if (optionalDomainLinkToAccountRequest.isPresent()) {
            DomainLinkToAccountRequest request = optionalDomainLinkToAccountRequest.get();

            Optional<Account> optionalAccount = accountRepository.findById(request.getAccountId());
            if (optionalAccount.isPresent()) {
                Account account = optionalAccount.get();

                request.setDecisionMadeBy(authentication.getName());
                request.setStatusUpdatedOn(ZonedDateTime.now());

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
                                temp.setAccount(account);
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

        info.setRequestedBy(request.getRequestedBy());
        if (request.getRequestedOn() != null) {
            info.setRequestedOn(request.getRequestedOn().toString());
        }

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

    @Override
    protected DomainLinkToAccountRequestForm entityToForm(DomainLinkToAccountRequest entity) {
        Account account = accountRepository.findById(entity.getAccountId())
                .orElse(null);
        List<Domain> domains = domainRepository.findAllByIdIn(entity.getRequestedDomainIds());
        List<DomainInfo> domainInfoList = domains.stream()
                .map(d -> new DomainInfo(d, true))
                .collect(Collectors.toList());

        DomainLinkToAccountRequestForm form = new DomainLinkToAccountRequestForm(entity);
        form.setDomainInfoList(domainInfoList);
        if(account != null) {
            form.setAccountInfo(new AccountInfo(account, false));
        }

        return form;
    }

    @Override
    protected DomainLinkToAccountRequest formToEntity(DomainLinkToAccountRequestForm form, Authentication authentication) throws Exception {
        Account account = accountRepository.findById(form.getAccountId())
                .orElseThrow(() -> new RAObjectNotFoundException(Account.class, form.getAccountId()));
        DomainLinkToAccountRequest request = DomainLinkToAccountRequest.buildNew();
        request.setAccount(account);
        request.setRequestedBy(authentication.getName());
        request.setRequestedOn(ZonedDateTime.now());

        List<Long> domainIds = new LinkedList<>(form.getRequestedDomainIds());
        domainIds.removeIf(Objects::isNull);
        form.setRequestedDomainIds(domainIds);

        List<Domain> requestedDomains = domainRepository.findAllByIdIn(form.getRequestedDomainIds());
        //Ensure user exists in the account
        Optional<PocEntry> pocEntryOptional = pocEntryRepository
                .findDistinctByEmailEqualsAndAccount(authentication.getName(), account);

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
            return request;
        } else {
            log.error("Requester not associated with account");
            throw new NotAuthorizedException(authentication.getName(), "Link Account Create");
        }
    }

    @Override
    protected DomainLinkToAccountRequest combine(DomainLinkToAccountRequest original, DomainLinkToAccountRequest updated, Authentication authentication) throws Exception {
        original.setStatus(updated.getStatus());
        return original;
    }

    @Override
    public List<Predicate> buildFilter(Map<String, String> allRequestParams, Root<DomainLinkToAccountRequest> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        return null;
    }
}
