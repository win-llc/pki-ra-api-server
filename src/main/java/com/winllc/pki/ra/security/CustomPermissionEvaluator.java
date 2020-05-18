package com.winllc.pki.ra.security;

import com.winllc.pki.ra.beans.form.ValidForm;
import com.winllc.pki.ra.domain.*;
import com.winllc.pki.ra.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    @Autowired
    private AccountRestrictionRepository accountRestrictionRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CertificateRequestRepository certificateRequestRepository;
    @Autowired
    private PocEntryRepository pocEntryRepository;
    @Autowired
    private ServerEntryRepository serverEntryRepository;
    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        boolean hasPermission = false;
        Object principal = authentication.getPrincipal();
        if(principal instanceof RAUser) {
            RAUser raUser = (RAUser) principal;

            //Super admin gets automatic access
            if(isSuperAdmin(raUser)) return true;

            List<String> permissions = raUser.getPermissions();
            String permissionString = (String) permission;

            if((permissions).contains(permissionString)){
                hasPermission = true;
            }

            //if for and edit/update operation, check if object tied to account if yes, make sure user a member on the account
            if (hasPermission && isEditDeleteOperation(permissionString)) {
                boolean isMemberOfAppropriateAccount = false;
                if (targetDomainObject instanceof ValidForm) {
                    ValidForm form = (ValidForm) targetDomainObject;
                    if (form.isAccountLinkedForm()) {

                        AtomicReference<Account> accountAtomic = new AtomicReference<>();

                        if (form.getFormObjectType().equals(AccountRestriction.class)) {
                            Optional<AccountRestriction> accountRestrictionOptional = accountRestrictionRepository.findById(form.getId());
                            accountRestrictionOptional.ifPresent(r -> accountAtomic.set(r.getAccount()));
                        }

                        if(form.getFormObjectType().equals(Account.class)){
                            Optional<Account> accountOptional = accountRepository.findById(form.getId());
                            accountOptional.ifPresent(a -> accountAtomic.set(a));
                        }

                        if(form.getFormObjectType().equals(CertificateRequest.class)){
                            Optional<CertificateRequest> certificateRequestOptional = certificateRequestRepository.findById(form.getId());
                            certificateRequestOptional.ifPresent(c -> accountAtomic.set(c.getAccount()));
                        }

                        if(form.getFormObjectType().equals(PocEntry.class)){
                            Optional<PocEntry> pocEntryOptional = pocEntryRepository.findById(form.getId());
                            pocEntryOptional.ifPresent(p -> accountAtomic.set(p.getAccount()));
                        }

                        if(form.getFormObjectType().equals(ServerEntry.class)){
                            Optional<ServerEntry> serverEntryOptional = serverEntryRepository.findById(form.getId());
                            serverEntryOptional.ifPresent(s -> accountAtomic.set(s.getAccount()));
                        }

                        //todo verify current user is a member of account
                        if (accountAtomic.get() != null) {
                            Account account = accountAtomic.get();

                            Optional<User> optionalUser = userRepository.findOneByUsername(raUser.getUsername());

                            List<Account> userAccounts = accountRepository.findAllByAccountUsersContains(optionalUser.get());
                            for(Account userAccount : userAccounts){
                                if(userAccount.getKeyIdentifier().contentEquals(account.getKeyIdentifier())){
                                    isMemberOfAppropriateAccount = true;
                                    break;
                                }
                            }
                        }
                    }
                }

                hasPermission = isMemberOfAppropriateAccount;
            }
        }

        return hasPermission;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        //todo
        return false;
    }

    private boolean isEditDeleteOperation(String permission){
        return permission.startsWith("update") || permission.startsWith("delete");
    }

    private boolean isSuperAdmin(RAUser raUser){
        List<String> permissions = raUser.getPermissions();
        return permissions.contains("super_admin");
    }

}
