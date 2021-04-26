package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.form.DomainForm;
import com.winllc.pki.ra.beans.info.DomainInfo;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.Domain;
import com.winllc.pki.ra.domain.DomainPolicy;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.DomainPolicyRepository;
import com.winllc.pki.ra.repository.DomainRepository;
import com.winllc.pki.ra.service.validators.DomainValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/domain")
public class DomainService {

    private static final Logger log = LogManager.getLogger(DomainService.class);

    private final DomainRepository domainRepository;
    private final DomainValidator domainValidator;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private DomainPolicyRepository domainPolicyRepository;

    public DomainService(DomainRepository domainRepository, DomainValidator domainValidator) {
        this.domainRepository = domainRepository;
        this.domainValidator = domainValidator;
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

    @GetMapping("/options/availableForAccount/{accountId}")
    @Transactional
    public List<DomainInfo> optionsAvailableForAccount(@PathVariable Long accountId) throws RAObjectNotFoundException {
        Optional<Account> accountOptional = accountRepository.findById(accountId);

        if(accountOptional.isPresent()){
            Account account = accountOptional.get();

            List<Domain> all = domainRepository.findAll();

            List<DomainPolicy> domainPolicies = domainPolicyRepository.findAllByAccount(account);

            return all.stream()
                    .filter(d -> domainPolicies.stream().noneMatch(dp -> dp.getTargetDomain().getId() == d.getId()))
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

    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public List<Domain> getAllAvailableDomains(){
        List<Domain> all = domainRepository.findAll();

        return all;
    }

    @GetMapping("/searchByBase/{search}")
    @ResponseStatus(HttpStatus.OK)
    public List<Domain> searchDomainByBaseDomain(@PathVariable String search){
        return domainRepository.findAllByBaseContains(search);
    }

    @PreAuthorize("hasPermission(#id, 'com.winllc.pki.ra.domain.Domain', 'view_domain')")
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
    public Long createDomain(@Valid @RequestBody DomainForm form) throws RAObjectNotFoundException {
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

        return domain.getId();
    }

    @PreAuthorize("hasPermission(#form, 'update_domain')")
    @PostMapping("/update")
    @ResponseStatus(HttpStatus.OK)
    public Domain updateDomain(@Valid @RequestBody DomainForm form) throws RAObjectNotFoundException{
        Optional<Domain> optionalDomain = domainRepository.findById(form.getId());
        if(optionalDomain.isPresent()){
            Domain existing = optionalDomain.get();
            existing.setBase(form.getBase());
            existing = domainRepository.save(existing);
            return existing;
        }else{
            throw new RAObjectNotFoundException(form);
        }
    }

    @PreAuthorize("hasPermission(#id, 'com.winllc.pki.ra.domain.Domain', 'delete_domain')")
    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteDomain(@PathVariable Long id){
        domainRepository.deleteById(id);
    }

}
