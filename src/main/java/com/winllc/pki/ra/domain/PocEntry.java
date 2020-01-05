package com.winllc.pki.ra.domain;

import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
public class PocEntry extends AbstractPersistable<Long> {

    private String email;
    private boolean enabled;
    private Timestamp addedOn;
    @ManyToOne
    private Account account;

    public static PocEntry buildNew(String email, Account account){
        PocEntry entry = new PocEntry();
        entry.setEmail(email);
        entry.setAccount(account);
        entry.setAddedOn(Timestamp.valueOf(LocalDateTime.now()));
        entry.setEnabled(true);
        return entry;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
