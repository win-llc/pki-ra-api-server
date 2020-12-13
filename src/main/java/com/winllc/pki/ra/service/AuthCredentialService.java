package com.winllc.pki.ra.service;

import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.AuthCredential;
import com.winllc.pki.ra.domain.AuthCredentialHolder;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.AuthCredentialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AuthCredentialService {

    private final AuthCredentialRepository authCredentialRepository;

    public AuthCredentialService(AuthCredentialRepository authCredentialRepository) {
        this.authCredentialRepository = authCredentialRepository;
    }

    public Optional<AuthCredential> getLatestAuthCredentialForAccount(Account account){
        List<AuthCredential> authCredentials = authCredentialRepository.findAllByParentEntity(account);
        return authCredentials.stream()
                .sorted((c1, c2) -> c1.getCreatedOn().compareTo(c2.getCreatedOn()))
                .findFirst();
    }

    public Optional<Account> getAssociatedAccount(String kid) throws RAObjectNotFoundException {
        Optional<AuthCredential> optionalAuthCredential = authCredentialRepository.findDistinctByKeyIdentifier(kid);

        if(optionalAuthCredential.isPresent()){
            AuthCredential authCredential = optionalAuthCredential.get();
            AuthCredentialHolder holder = authCredential.getParentEntity();

            if(holder instanceof Account){
                Account account = (Account) holder;
                return Optional.of(account);
            }else{
                //todo handler server entry
                return Optional.empty();
            }
        }else{
            throw new RAObjectNotFoundException(AuthCredential.class, kid);
        }
    }
}
