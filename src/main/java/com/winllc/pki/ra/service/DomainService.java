package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.form.AccountUpdateForm;
import com.winllc.pki.ra.beans.form.DomainForm;
import com.winllc.pki.ra.beans.info.DomainInfo;
import com.winllc.acme.common.domain.Account;
import com.winllc.acme.common.domain.Domain;
import com.winllc.acme.common.domain.DomainPolicy;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.AccountRepository;
import com.winllc.acme.common.repository.DomainPolicyRepository;
import com.winllc.acme.common.repository.DomainRepository;
import com.winllc.pki.ra.service.validators.DomainValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/domain")
public class DomainService extends DataPagedService<Domain, DomainForm, DomainRepository> {

    private static final Logger log = LogManager.getLogger(DomainService.class);

    private final DomainRepository domainRepository;
    private final DomainValidator domainValidator;
    private final AccountRepository accountRepository;
    private final DomainPolicyRepository domainPolicyRepository;

    public DomainService(ApplicationContext applicationContext,
                         DomainRepository domainRepository, DomainValidator domainValidator, AccountRepository accountRepository, DomainPolicyRepository domainPolicyRepository) {
        super(applicationContext, Domain.class, domainRepository);
        this.domainRepository = domainRepository;
        this.domainValidator = domainValidator;
        this.accountRepository = accountRepository;
        this.domainPolicyRepository = domainPolicyRepository;
    }

    @InitBinder("domainForm")
    public void initBinder(WebDataBinder binder) {
        binder.setValidator(domainValidator);
    }

    @GetMapping("/options")
    public Map<Long, String> options(){
        List<Domain> all = domainRepository.findAll();
        return all.stream()
                .collect(Collectors.toMap(d -> d.getId(), d -> d.getFullDomainName()));
    }

    @GetMapping("/options/forAccount/{accountId}/map")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public Map<Long, String> optionsForAccountMap(@PathVariable Long accountId, Authentication authentication) throws RAObjectNotFoundException {
        List<DomainInfo> pocs = optionsForAccount(accountId);
        return pocs.stream()
                .collect(Collectors.toMap(d -> d.getId(), d -> d.getFullDomainName()));
    }

    @GetMapping("/options/availableForAccount/{accountId}")
    @Transactional
    public List<DomainInfo> optionsAvailableForAccount(@PathVariable Long accountId) throws RAObjectNotFoundException {
        Optional<Account> accountOptional = accountRepository.findById(accountId);

        if(accountOptional.isPresent()){
            Account account = accountOptional.get();

            List<Domain> all = domainRepository.findAll();

            List<DomainPolicy> domainPolicies = domainPolicyRepository.findAllByAccount(account);

            return all.stream()
                    .filter(d -> domainPolicies.stream().noneMatch(dp -> Objects.equals(dp.getTargetDomain().getId(), d.getId())))
                    .map(d -> new DomainInfo(d, false))
                    .collect(Collectors.toList());
        }else{
            throw new RAObjectNotFoundException(Account.class, accountId);
        }
    }

    @GetMapping("/options/forAccount/{accountId}")
    @Transactional
    public List<DomainInfo> optionsForAccount(@PathVariable Long accountId) throws RAObjectNotFoundException {

        Optional<Account> accountOptional = accountRepository.findById(accountId);

        if(accountOptional.isPresent()){
            Account account = accountOptional.get();

            List<DomainPolicy> domainPolicies = domainPolicyRepository.findAllByAccount(account);

            return domainPolicies.stream()
                    .map(d -> new DomainInfo(d.getTargetDomain(), false))
                    .collect(Collectors.toList());
        }else{
            throw new RAObjectNotFoundException(Account.class, accountId);
        }
    }

    @GetMapping("/searchByBase/{search}")
    @ResponseStatus(HttpStatus.OK)
    public List<Domain> searchDomainByBaseDomain(@PathVariable String search){
        return domainRepository.findAllByBaseContains(search);
    }

    @PreAuthorize("hasPermission(#id, 'com.winllc.acme.common.domain.Domain', 'view_domain')")
    @GetMapping("/byId/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public DomainInfo getDomainById(@PathVariable Long id) throws RAObjectNotFoundException {
        Optional<Domain> optionalDomain = domainRepository.findById(id);
        if(optionalDomain.isPresent()){
            Domain domain = optionalDomain.get();
            return new DomainInfo(domain, true);
        }else{
            throw new RAObjectNotFoundException(Domain.class, id);
        }
    }

    @PreAuthorize("hasPermission(#form, 'add_domain')")
    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Long createDomain(@Valid @RequestBody DomainForm form, Authentication authentication)
            throws Exception {
        DomainForm added = add(form, authentication);

        return added.getId();
    }


    @Override
    public DomainForm entityToForm(Domain entity) {
        return new DomainForm(entity);
    }

    @Override
    protected Domain formToEntity(DomainForm form, Authentication authentication) throws RAObjectNotFoundException {
        Domain domain = new Domain();
        domain.setBase(form.getBase());

        if(form.getParentDomainId() != null){
            Optional<Domain> optionalDomain = domainRepository.findById(form.getParentDomainId());
            if(optionalDomain.isPresent()){
                Domain parentDomain = optionalDomain.get();
                domain.setParentDomain(parentDomain);

                String newBase = domain.getBase()+"."+parentDomain.getFullDomainName();
                domain.setFullDomainName(newBase);

                domain = domainRepository.save(domain);

                parentDomain.getSubDomains().add(domain);
                domainRepository.save(parentDomain);
            }else{
                throw new RAObjectNotFoundException(Domain.class, form.getParentDomainId());
            }
        }else{
            domain.setFullDomainName(form.getBase());
        }

        domain = domainRepository.save(domain);
        return domain;
    }

    @Override
    protected Domain combine(Domain original, Domain updated, Authentication authentication) {
        original.setBase(updated.getBase());
        return original;
    }

    @Override
    public List<Predicate> buildFilter(Map<String, String> allRequestParams, Root<Domain> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        return null;
    }
}
