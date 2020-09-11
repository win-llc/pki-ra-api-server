package com.winllc.pki.ra.security;

import com.winllc.pki.ra.beans.form.ValidForm;
import com.winllc.pki.ra.domain.*;
import com.winllc.pki.ra.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    private final AccountRestrictionRepository accountRestrictionRepository;
    private final AccountRepository accountRepository;
    private final CertificateRequestRepository certificateRequestRepository;
    private final PocEntryRepository pocEntryRepository;
    private final ServerEntryRepository serverEntryRepository;

    public CustomPermissionEvaluator(AccountRestrictionRepository accountRestrictionRepository,
                                     AccountRepository accountRepository, CertificateRequestRepository certificateRequestRepository,
                                     PocEntryRepository pocEntryRepository, ServerEntryRepository serverEntryRepository) {
        this.accountRestrictionRepository = accountRestrictionRepository;
        this.accountRepository = accountRepository;
        this.certificateRequestRepository = certificateRequestRepository;
        this.pocEntryRepository = pocEntryRepository;
        this.serverEntryRepository = serverEntryRepository;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        boolean hasPermission = false;
        Object principal = authentication.getPrincipal();
        if(principal instanceof UserDetails) {
            UserDetails raUser = (UserDetails) principal;
            List<String> permissions = raUser.getAuthorities().stream().map(ga -> ga.toString()).collect(Collectors.toList());

            //Super admin gets automatic access
            if(isSuperAdmin(raUser, permissions)) return true;

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

                            Optional<PocEntry> pocOptional = pocEntryRepository.findDistinctByEmailEqualsAndAccount(raUser.getUsername(), account);

                            if(pocOptional.isPresent()){
                                isMemberOfAppropriateAccount = true;
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

    private boolean isSuperAdmin(UserDetails raUser, List<String> permissions){
        return permissions.contains("super_admin");
    }

}
