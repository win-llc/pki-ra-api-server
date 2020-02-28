package com.winllc.pki.ra.security;

import com.winllc.pki.ra.beans.form.ValidForm;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.AccountRestriction;
import com.winllc.pki.ra.repository.AccountRestrictionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    @Autowired
    private AccountRestrictionRepository accountRestrictionRepository;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {

        boolean isMemberOfAppropriateAccount = false;
        if(targetDomainObject instanceof ValidForm){
            ValidForm form = (ValidForm) targetDomainObject;
            if(form.isAccountLinkedForm()){

                AtomicReference<Account> accountAtomic = new AtomicReference<>();

                if(form.getFormObjectType().equals(AccountRestriction.class)){
                    Optional<AccountRestriction> accountRestrictionOptional = accountRestrictionRepository.findById(form.getId());
                    accountRestrictionOptional.ifPresent(r -> accountAtomic.set(r.getAccount()));
                }

                //todo verify current user is a member of account
                if(accountAtomic.get() != null) {
                    Account account = accountAtomic.get();
                    account.getAccountUsers();
                }
            }
        }

        if(isMemberOfAppropriateAccount){
            //check permissions


        }

        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return false;
    }


    private boolean checkServerEntryPermissions(){
        //todo
        return false;
    }

    private boolean checkAccountRestrictionPermissions(){
        //todo
        return false;
    }

    private boolean checkCertificateRequestPermissions(){
        //todo
        return false;
    }

    private boolean checkAccountPermissions(){
        //todo
        return false;
    }
}
