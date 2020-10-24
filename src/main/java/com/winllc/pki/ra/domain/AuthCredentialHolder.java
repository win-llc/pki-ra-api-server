package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;
import java.util.Optional;
import java.util.Set;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class AuthCredentialHolder extends UniqueEntity {

    @JsonIgnore
    public abstract Set<AuthCredential> getAuthCredentials();
    @JsonIgnore
    public Optional<AuthCredential> getLatestAuthCredential(){
        return getAuthCredentials().stream()
                .sorted()
                .findFirst();
    }
}
