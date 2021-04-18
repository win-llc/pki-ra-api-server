package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.form.UniqueEntityLookupForm;
import com.winllc.pki.ra.domain.*;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.AuthCredentialRepository;
import com.winllc.pki.ra.repository.ServerEntryRepository;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
            throws RAObjectNotFoundException, ClassNotFoundException {
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
        }

        if(holder != null){
            return authCredentialRepository.findAllByParentEntityAndValidEquals(holder, true);
        }else{
            throw new RAObjectNotFoundException(ServerEntry.class, lookupForm.getObjectUuid());
        }
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
