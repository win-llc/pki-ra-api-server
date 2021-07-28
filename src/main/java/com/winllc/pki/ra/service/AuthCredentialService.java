package com.winllc.pki.ra.service;

import com.winllc.acme.common.domain.Account;
import com.winllc.pki.ra.beans.form.AuthCredentialsUpdateForm;
import com.winllc.pki.ra.beans.form.UniqueEntityLookupForm;
import com.winllc.acme.common.domain.*;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.AccountRepository;
import com.winllc.acme.common.repository.AuthCredentialRepository;
import com.winllc.acme.common.repository.ServerEntryRepository;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Hibernate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/authCredential")
public class AuthCredentialService {

    private final AuthCredentialRepository authCredentialRepository;
    private final ServerEntryRepository serverEntryRepository;
    private final AccountRepository accountRepository;

    public AuthCredentialService(AuthCredentialRepository authCredentialRepository, ServerEntryRepository serverEntryRepository, AccountRepository accountRepository) {
        this.authCredentialRepository = authCredentialRepository;
        this.serverEntryRepository = serverEntryRepository;
        this.accountRepository = accountRepository;
    }

    @PostMapping("/validAuthCredentials")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<AuthCredential> getValidAuthCredentials(@Valid @RequestBody UniqueEntityLookupForm lookupForm)
            throws Exception {

        AuthCredentialHolder holder = getHolder(lookupForm);

        if(holder != null){
            return authCredentialRepository.findAllByParentEntity(holder);
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
                    AuthCredentialHolder holder = getHolder(form.getLookupForm());
                    AuthCredential newCred = AuthCredential.buildNew(holder);
                    authCredentialRepository.save(newCred);
                }
            }
        }

        return getValidAuthCredentials(form.getLookupForm());
    }

    public AuthCredentialHolder getHolder(UniqueEntityLookupForm lookupForm) throws Exception {
        String entityClass = lookupForm.getObjectClass();
        Class<?> clazz = Class.forName(entityClass);

        AuthCredentialHolder holder = null;
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

    public AuthCredentialHolder addNewAuthCredentialToEntry(AuthCredentialHolder holder) {
        AuthCredential authCredential = AuthCredential.buildNew(holder);

        authCredential = authCredentialRepository.save(authCredential);

        Hibernate.initialize(holder.getAuthCredentials());
        holder.getAuthCredentials().add(authCredential);
        return holder;
    }

    public Optional<AuthCredential> getLatestAuthCredentialForAccount(Account account){
        List<AuthCredential> authCredentials = authCredentialRepository.findAllByParentEntity(account);
        return authCredentials.stream()
                .sorted(Comparator.comparing(AuthCredential::getCreatedOn))
                .findFirst();
    }

    @Transactional
    public Optional<Account> getAssociatedAccount(String kid) throws RAObjectNotFoundException {
        Optional<AuthCredential> optionalAuthCredential = authCredentialRepository.findDistinctByKeyIdentifier(kid);

        if(optionalAuthCredential.isPresent()){
            AuthCredential authCredential = optionalAuthCredential.get();
            AuthCredentialHolder holder = authCredential.getParentEntity();

            if(holder instanceof Account){
                Account account = (Account) holder;
                return Optional.of(account);
            }else if(holder instanceof ServerEntry){
                Optional<ServerEntry> optionalServerEntry = serverEntryRepository.findById(holder.getId());

                if(optionalServerEntry.isPresent()){
                    ServerEntry serverEntry = optionalServerEntry.get();
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
}
