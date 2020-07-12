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
@RequestMapping("/api/domainRestriction")
public class DomainCertIssuanceRestrictionService {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private DomainPolicyRepository restrictionRepository;

    @GetMapping("/byType/{type}/{id}")
    public Set<DomainPolicyForm> getRestrictionsForType(@PathVariable String type, @PathVariable Long id)
            throws Exception {

        DomainCertIssuanceRestrictionHolder holder = getTargetObject(type, id);
        Set<DomainPolicy> restrictionSet = holder.getDomainIssuanceRestrictions();


        return restrictionSet.stream()
                .map(DomainPolicyForm::new)
                .collect(Collectors.toSet());
    }

    @PostMapping("/addRestrictionForType/{type}/{targetId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Long addRestrictionForType(@PathVariable String type, @PathVariable Long targetId,
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

    @DeleteMapping("/deleteRestrictionForType/{type}/{targetId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public void deleteRestrictionForType(@PathVariable String type, @PathVariable Long targetId,
                                         Long restrictionId) throws Exception {
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

    //todo
    private DomainPolicyForm buildForm(DomainPolicy domainPolicy){
        Domain domain = domainPolicy.getTargetDomain();
        DomainPolicyForm form = new DomainPolicyForm(domainPolicy);
        if(!CollectionUtils.isEmpty(domain.getSubDomains())){
            for(Domain subDomain : domain.getSubDomains()){
                //DomainPolicyForm temp = buildForm(subDomain);
                //form.getSubDomainForms().add(temp);
            }
        }
        return form;
    }
}
