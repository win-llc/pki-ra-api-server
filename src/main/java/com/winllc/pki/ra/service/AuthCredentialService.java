package com.winllc.pki.ra.service;

import com.winllc.acme.common.domain.Account;
import com.winllc.pki.ra.beans.form.AuthCredentialForm;
import com.winllc.pki.ra.beans.form.AuthCredentialsUpdateForm;
import com.winllc.pki.ra.beans.form.UniqueEntityLookupForm;
import com.winllc.acme.common.domain.*;
import com.winllc.pki.ra.beans.search.GridFilterModel;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.AccountRepository;
import com.winllc.acme.common.repository.AuthCredentialRepository;
import com.winllc.acme.common.repository.ServerEntryRepository;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Hibernate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import javax.validation.Valid;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/authCredential")
public class AuthCredentialService extends UpdatedDataPagedService<AuthCredential,
        AuthCredentialForm, AuthCredentialRepository> {

    private final AuthCredentialRepository authCredentialRepository;
    private final ServerEntryRepository serverEntryRepository;
    private final AccountRepository accountRepository;

    public AuthCredentialService(ApplicationContext context,
                                 AuthCredentialRepository authCredentialRepository,
                                 ServerEntryRepository serverEntryRepository,
                                 AccountRepository accountRepository) {
        super(context, AuthCredential.class, authCredentialRepository);
        this.authCredentialRepository = authCredentialRepository;
        this.serverEntryRepository = serverEntryRepository;
        this.accountRepository = accountRepository;
    }

    @PostMapping("/validAuthCredentials")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<AuthCredential> getValidAuthCredentials(@Valid @RequestBody UniqueEntityLookupForm lookupForm)
            throws Exception {

        AuthCredentialHolderInteface holder = getHolder(lookupForm);

        if(holder != null){
            if(holder instanceof Account){
                return authCredentialRepository.findAllByAccount((Account) holder);
            }else{
                return authCredentialRepository.findAllByServerEntry((ServerEntry) holder);
            }

        }else{
            throw new RAObjectNotFoundException(ServerEntry.class, lookupForm.getObjectUuid());
        }
    }

    @PostMapping("/updateAuthCredentials")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<AuthCredential> updateAuthCredential(@RequestBody AuthCredentialsUpdateForm form)
            throws Exception {

        if(CollectionUtils.isNotEmpty(form.getAuthCredentials())){
            for(AuthCredential authCredential : form.getAuthCredentials()){
                boolean exists = false;
                if(authCredential.getId() != null) {
                    Optional<AuthCredential> optionalCred = authCredentialRepository.findById(authCredential.getId());
                    if(optionalCred.isPresent()){
                        exists = true;
                        AuthCredential existing = optionalCred.get();
                        existing.setValid(authCredential.getValid());
                        existing.setExpiresOn(authCredential.getExpiresOn());
                        authCredentialRepository.save(existing);
                    }
                }
                if(!exists){
                    AuthCredentialHolderInteface holder = getHolder(form.getLookupForm());
                    AuthCredential newCred = AuthCredential.buildNew(holder);
                    authCredentialRepository.save(newCred);
                }
            }
        }

        return getValidAuthCredentials(form.getLookupForm());
    }

    public AuthCredentialHolderInteface getHolder(UniqueEntityLookupForm lookupForm) throws Exception {
        String entityClass = lookupForm.getObjectClass();
        Class<?> clazz = Class.forName(entityClass);

        AuthCredentialHolderInteface holder = null;
        if(clazz == ServerEntry.class){
            Optional<ServerEntry> optionalServerEntry = serverEntryRepository.findDistinctByUuidEquals(lookupForm.getObjectUuid());
            if(optionalServerEntry.isPresent()){
                holder = optionalServerEntry.get();
            }
        }else if(clazz == Account.class){
            Optional<Account> optionalAccount = accountRepository.findDistinctByUuidEquals(lookupForm.getObjectUuid());
            if(optionalAccount.isPresent()){
                holder = optionalAccount.get();
            }
        }else{
            throw new Exception("Could not find a valid AuthCredentialHolder for: "+lookupForm);
        }

        return holder;
    }

    public AuthCredentialHolderInteface addNewAuthCredentialToEntry(AuthCredentialHolderInteface holder) {
        AuthCredential authCredential = AuthCredential.buildNew(holder);

        authCredential = authCredentialRepository.save(authCredential);

        Hibernate.initialize(holder.getAuthCredentials());
        holder.getAuthCredentials().add(authCredential);
        return holder;
    }

    public Optional<AuthCredential> getLatestAuthCredentialForAccount(Account account){
        List<AuthCredential> authCredentials = authCredentialRepository.findAllByAccount(account);
        return authCredentials.stream()
                .sorted(Comparator.comparing(AuthCredential::getCreatedOn))
                .findFirst();
    }

    @Transactional
    public Optional<Account> getAssociatedAccount(String kid) throws RAObjectNotFoundException {
        Optional<AuthCredential> optionalAuthCredential = authCredentialRepository.findDistinctByKeyIdentifier(kid);

        if(optionalAuthCredential.isPresent()){
            AuthCredential authCredential = optionalAuthCredential.get();
            Optional<AuthCredentialHolderInteface> parentEntity = authCredential.getParentEntity();
            AuthCredentialHolderInteface holder = parentEntity.orElseGet(() -> null);

            if(holder instanceof Account account){
                return Optional.of(account);
            }else if(holder instanceof ServerEntry){
                ServerEntry serverEntry = (ServerEntry) holder;
                Optional<ServerEntry> optionalServerEntry = serverEntryRepository.findById(serverEntry.getId());

                if(optionalServerEntry.isPresent()){
                    serverEntry = optionalServerEntry.get();
                    Hibernate.initialize(serverEntry.getAccount());
                    return Optional.of(serverEntry.getAccount());
                }else{
                    return Optional.empty();
                }
            }else{
                return Optional.empty();
            }
        }else{
            throw new RAObjectNotFoundException(AuthCredential.class, kid);
        }
    }

    @Override
    protected void postSave(AuthCredential entity, AuthCredentialForm form) {

    }

    @Override
    protected AuthCredentialForm entityToForm(AuthCredential entity, Authentication authentication) {
        return new AuthCredentialForm(entity);
    }

    @Override
    protected AuthCredential formToEntity(AuthCredentialForm form, Map<String, String> params,
                                          Authentication authentication) throws Exception {
        Optional<AuthCredentialHolderInteface> parentObject = getParentObject(params);

        if(parentObject.isPresent()) {
            AuthCredential authCredential = AuthCredential.buildNew(parentObject.get());
            authCredential.setValid(form.isValid());

            return authCredential;
        }else{
            throw new Exception("Could not find parent object");
        }
    }

    @Override
    protected AuthCredential combine(AuthCredential original, AuthCredential updated, Authentication authentication) throws Exception {
        original.setValid(updated.getValid());
        return original;
    }

    @Override
    public List<Predicate> buildFilter(Map<String, String> allRequestParams, GridFilterModel filterModel, Root<AuthCredential> root, CriteriaQuery<?> query, CriteriaBuilder cb, Authentication authentication) {
        return null;
    }
}
