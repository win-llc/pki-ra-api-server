package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nimbusds.jose.util.Base64;
import com.winllc.pki.ra.util.AppUtil;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@Entity
@Table(name = "auth_credential")
public class AuthCredential extends AbstractPersistable<Long> implements Comparable<AuthCredential> {

    @Column(unique = true)
    private String keyIdentifier;
    private String macKey;
    private Boolean valid;
    private String pocAssignedTo;
    @Column(nullable = false)
    private Timestamp createdOn;
    private Timestamp expiresOn;
    @ManyToOne
    @JoinColumn(name="parentEntity_fk")
    private AuthCredentialHolder parentEntity;

    public static AuthCredential buildNew(AuthCredentialHolder credentialHolder){
        AuthCredential authCredential = new AuthCredential();

        String macKey = AppUtil.generate256BitString();
        String keyIdentifier = AppUtil.generate20BitString();

        authCredential.setMacKey(macKey);
        authCredential.setKeyIdentifier(keyIdentifier);
        authCredential.setValid(true);
        authCredential.setParentEntity(credentialHolder);
        authCredential.setCreatedOn(Timestamp.valueOf(LocalDateTime.now()));

        return authCredential;
    }

    @PreRemove
    private void preRemove(){
        if(parentEntity != null) {
            parentEntity.getAuthCredentials().remove(this);
        }
    }

    @JsonIgnore
    public <T extends AuthCredentialHolder> Optional<T> getParentEntityByType(Class<T> clazz){
        if(getParentEntity().getClass().isAssignableFrom(clazz)){
            return Optional.of((T) getParentEntity());
        }else{
            return Optional.empty();
        }
    }

    public String getMacKeyBase64(){
        return Base64.encode(this.macKey).toString();
    }

    public String getKeyIdentifier() {
        return keyIdentifier;
    }

    public void setKeyIdentifier(String keyIdentifier) {
        this.keyIdentifier = keyIdentifier;
    }

    public String getMacKey() {
        return macKey;
    }

    public void setMacKey(String macKey) {
        this.macKey = macKey;
    }

    public String getPocAssignedTo() {
        return pocAssignedTo;
    }

    public void setPocAssignedTo(String pocAssignedTo) {
        this.pocAssignedTo = pocAssignedTo;
    }

    public Boolean getValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    public Timestamp getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Timestamp createdOn) {
        this.createdOn = createdOn;
    }

    public Timestamp getExpiresOn() {
        return expiresOn;
    }

    public void setExpiresOn(Timestamp expiresOn) {
        this.expiresOn = expiresOn;
    }

    public AuthCredentialHolder getParentEntity() {
        return parentEntity;
    }

    public void setParentEntity(AuthCredentialHolder parentEntity) {
        this.parentEntity = parentEntity;
    }

    @Override
    public int compareTo(AuthCredential o) {
        return this.createdOn.compareTo(o.createdOn);
    }
}
