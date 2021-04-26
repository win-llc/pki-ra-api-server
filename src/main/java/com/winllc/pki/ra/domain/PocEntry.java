package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.winllc.acme.common.domain.BaseEntity;
import org.apache.commons.collections.CollectionUtils;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "poc_entry")
public class PocEntry extends BaseEntity implements AccountOwnedEntity {

    private String email;
    private boolean groupEmail;
    private boolean enabled;
    private boolean owner;
    private boolean canManageAllServers;
    private boolean addedManually;
    private Timestamp addedOn;
    @ManyToOne
    @JoinColumn(name="accountOwner_fk")
    private Account account;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinTable(name = "pocs_servers",
            joinColumns = {
                    @JoinColumn(name = "poc_id", referencedColumnName = "id",
                            nullable = false, updatable = false)},
            inverseJoinColumns = {
                    @JoinColumn(name = "server_id", referencedColumnName = "id",
                            nullable = false, updatable = false)})
    private Set<ServerEntry> manages;

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

        if(CollectionUtils.isNotEmpty(manages)){
            for(ServerEntry serverEntry : manages){
                serverEntry.getManagedBy().remove(this);
            }
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

    public boolean isOwner() {
        return owner;
    }

    public void setOwner(boolean owner) {
        this.owner = owner;
    }

    public boolean isCanManageAllServers() {
        return canManageAllServers;
    }

    public void setCanManageAllServers(boolean canManageAllServers) {
        this.canManageAllServers = canManageAllServers;
    }

    public boolean isAddedManually() {
        return addedManually;
    }

    public void setAddedManually(boolean addedManually) {
        this.addedManually = addedManually;
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

    public Set<ServerEntry> getManages() {
        return manages;
    }

    public void setManages(Set<ServerEntry> manages) {
        this.manages = manages;
    }
}
