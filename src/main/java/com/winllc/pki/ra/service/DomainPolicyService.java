package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.form.DomainPolicyForm;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.Domain;
import com.winllc.pki.ra.domain.DomainPolicy;
import com.winllc.pki.ra.domain.DomainCertIssuanceRestrictionHolder;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.DomainPolicyRepository;
import com.winllc.pki.ra.repository.DomainRepository;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/domainPolicy")
public class DomainPolicyService {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private DomainPolicyRepository restrictionRepository;

    @GetMapping("/byType/{type}/{id}")
    @Transactional
    public Set<DomainPolicyForm> getRestrictionsForType(@PathVariable String type, @PathVariable Long id)
            throws Exception {

        DomainCertIssuanceRestrictionHolder holder = getTargetObject(type, id);
        Set<DomainPolicy> restrictionSet = holder.getDomainIssuanceRestrictions();


        return restrictionSet.stream()
                .map(DomainPolicyForm::new)
                .collect(Collectors.toSet());
    }

    @PostMapping("/addForType/{type}/{targetId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Long addForType(@PathVariable String type, @PathVariable Long targetId,
                                      DomainPolicyForm form) throws Exception {
        DomainCertIssuanceRestrictionHolder holder = getTargetObject(type, targetId);

        Optional<Domain> optionalDomain = domainRepository.findById(form.getDomainId());

        if(optionalDomain.isPresent()) {
            Domain domain = optionalDomain.get();

            DomainPolicy restriction = new DomainPolicy();
            restriction.setTargetDomain(domain);
            restriction.setAcmeRequireDnsValidation(form.isAcmeRequireDnsValidation());
            restriction.setAcmeRequireHttpValidation(form.isAcmeRequireHttpValidation());
            restriction.setAllowIssuance(form.isAllowIssuance());

            restriction = restrictionRepository.save(restriction);

            domain.getAllDomainPolicies().add(restriction);
            holder.getDomainIssuanceRestrictions().add(restriction);

            domainRepository.save(domain);
            saveHolder(holder);

            return restriction.getId();
        }else{
            throw new RAObjectNotFoundException(Domain.class, form.getDomainId());
        }
    }

    @PostMapping("/updateForType")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public DomainPolicyForm updateForType(@RequestBody DomainPolicyForm form) throws RAObjectNotFoundException {

        Optional<DomainPolicy> optionalDomainPolicy = restrictionRepository.findById(form.getId());
        if(optionalDomainPolicy.isPresent()){
            DomainPolicy domainPolicy = optionalDomainPolicy.get();
            domainPolicy.setAcmeRequireDnsValidation(form.isAcmeRequireDnsValidation());
            domainPolicy.setAcmeRequireHttpValidation(form.isAcmeRequireHttpValidation());
            domainPolicy.setAllowIssuance(form.isAllowIssuance());
            domainPolicy = restrictionRepository.save(domainPolicy);

            return new DomainPolicyForm(domainPolicy);
        }else{
            throw new RAObjectNotFoundException(DomainPolicy.class, form.getId());
        }
    }

    @DeleteMapping("/deleteForType/{type}/{targetId}/{restrictionId}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public void deleteForType(@PathVariable String type, @PathVariable Long targetId,
                                         @PathVariable Long restrictionId) throws Exception {
        DomainCertIssuanceRestrictionHolder holder = getTargetObject(type, targetId);

        Optional<DomainPolicy> optionalRestriction = restrictionRepository.findById(restrictionId);
        if(optionalRestriction.isPresent()){
            DomainPolicy restriction = optionalRestriction.get();
            holder.getDomainIssuanceRestrictions().remove(restriction);
            saveHolder(holder);

            restrictionRepository.deleteById(restrictionId);
        }else{
            throw new RAObjectNotFoundException(DomainPolicy.class, restrictionId);
        }
    }

    private void saveHolder(DomainCertIssuanceRestrictionHolder holder) throws Exception {
        if(holder instanceof Account){
            accountRepository.save((Account) holder);
        }else if(holder instanceof Domain){
            domainRepository.save((Domain) holder);
        }else{
            throw new Exception("Holder not supported: "+holder.getClass().getCanonicalName());
        }
    }

    private DomainCertIssuanceRestrictionHolder getTargetObject(String type, Long targetId) throws Exception {
        List<DomainPolicy> restrictions = new ArrayList<>();
        DomainCertIssuanceRestrictionHolder holder = null;
        switch (type){
            case "account":
                Optional<Account> optionalAccount = accountRepository.findById(targetId);
                if(optionalAccount.isPresent()){
                    Account account = optionalAccount.get();
                    Hibernate.initialize(account.getAccountDomainPolicies());
                    holder = account;
                }else{
                    throw new RAObjectNotFoundException(Account.class, targetId);
                }
                break;
            case "domain":
                Optional<Domain> optionalDomain = domainRepository.findById(targetId);
                if(optionalDomain.isPresent()){
                    Domain domain = optionalDomain.get();
                    Hibernate.initialize(domain.getGlobalDomainPolicy());
                    //holder = domain;
                }else{
                    throw new RAObjectNotFoundException(Domain.class, targetId);
                }
                break;
        }

        if(holder != null){
            return holder;
        }else{
            throw new Exception("No holder of type: "+ type + " and ID: "+targetId);
        }
    }

}
