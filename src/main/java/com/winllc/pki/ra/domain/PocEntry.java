package com.winllc.pki.ra.domain;

import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "poc_entry")
public class PocEntry extends AbstractPersistable<Long> implements AccountOwnedEntity {

    private String email;
    private boolean groupEmail;
    private boolean enabled;
    private Timestamp addedOn;
    @ManyToOne
    @JoinColumn(name="accountOwner_fk")
    private Account account;

    public static PocEntry buildNew(String email, Account account){
        PocEntry entry = new PocEntry();
        entry.setEmail(email);
        entry.setAccount(account);
        entry.setAddedOn(Timestamp.valueOf(LocalDateTime.now()));
        entry.setEnabled(true);
        return entry;
    }

    @PreRemove
    private void preRemove(){
        if(account != null){
            account.getPocs().remove(this);
        }
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isGroupEmail() {
        return groupEmail;
    }

    public void setGroupEmail(boolean groupEmail) {
        this.groupEmail = groupEmail;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Timestamp getAddedOn() {
        return addedOn;
    }

    public void setAddedOn(Timestamp addedOn) {
        this.addedOn = addedOn;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }
}
