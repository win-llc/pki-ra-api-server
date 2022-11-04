package com.winllc.pki.ra.service;

import com.winllc.acme.common.domain.*;
import com.winllc.pki.ra.beans.form.DomainPolicyForm;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.AccountRepository;
import com.winllc.acme.common.repository.DomainPolicyRepository;
import com.winllc.acme.common.repository.DomainRepository;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Hibernate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/domainPolicy")
public class DomainPolicyService extends AccountDataTableService<DomainPolicy, DomainPolicyForm> {

    private final AccountRepository accountRepository;
    private final DomainRepository domainRepository;
    private final DomainPolicyRepository restrictionRepository;

    public DomainPolicyService(ApplicationContext context, AccountRepository accountRepository,
                               DomainRepository domainRepository,
                               DomainPolicyRepository restrictionRepository) {
        super(context, accountRepository, restrictionRepository);
        this.accountRepository = accountRepository;
        this.domainRepository = domainRepository;
        this.restrictionRepository = restrictionRepository;
    }

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

            //todo needs to work with domains also
            Optional<DomainPolicy> domainPolicy = checkEntityContainsDomainPolicy(holder, domain);

            if(domainPolicy.isEmpty()) {
                DomainPolicy restriction = new DomainPolicy();
                restriction.setTargetDomain(domain);
                restriction.setAcmeRequireDnsValidation(form.isAcmeRequireDnsValidation());
                restriction.setAcmeRequireHttpValidation(form.isAcmeRequireHttpValidation());
                restriction.setAllowIssuance(form.isAllowIssuance());

                if (holder instanceof Account) {
                    restriction.setAccount((Account) holder);
                }

                restriction = restrictionRepository.save(restriction);

                domain.getAllDomainPolicies().add(restriction);
                holder.getDomainIssuanceRestrictions().add(restriction);

                domainRepository.save(domain);
                saveHolder(holder);

                return restriction.getId();
            }else{
                return domainPolicy.get().getId();
            }
        }else{
            throw new RAObjectNotFoundException(Domain.class, form.getDomainId());
        }
    }

    private Optional<DomainPolicy> checkEntityContainsDomainPolicy(DomainCertIssuanceRestrictionHolder holder, Domain domain) throws Exception {
        if(holder instanceof Account){
            return restrictionRepository.findDistinctByAccountAndTargetDomain((Account) holder, domain);
        }else{
            throw new Exception("Holder not a valid type, Domain or Account");
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

    @PostMapping("/updateAllForType")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public Set<DomainPolicyForm> updateAllForType(@RequestBody List<DomainPolicyForm> forms){
        Set<DomainPolicyForm> updated = new HashSet<>();
        if(CollectionUtils.isNotEmpty(forms)){
            for(DomainPolicyForm form : forms){
                try {
                    updated.add(updateForType(form));
                } catch (RAObjectNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        return updated;
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

    @Override
    protected DomainPolicyForm entityToForm(DomainPolicy entity) {
        Hibernate.initialize(entity.getTargetDomain());
        return new DomainPolicyForm(entity);
    }

    @Override
    protected DomainPolicy formToEntity(DomainPolicyForm form, Account account) {
        DomainPolicy policy = new DomainPolicy();
        policy.setId(form.getId());
        policy.setAcmeRequireDnsValidation(form.isAcmeRequireDnsValidation());
        policy.setAcmeRequireHttpValidation(form.isAcmeRequireHttpValidation());
        policy.setAllowIssuance(form.isAllowIssuance());

        return policy;
    }

    @Override
    protected DomainPolicy combine(DomainPolicy original, DomainPolicy updated) {
        original.setAcmeRequireDnsValidation(updated.isAcmeRequireDnsValidation());
        original.setAcmeRequireHttpValidation(updated.isAcmeRequireHttpValidation());
        original.setAllowIssuance(updated.isAllowIssuance());
        return original;
    }
}
