package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.winllc.acme.common.domain.BaseEntity;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "entity_permission")
public class EntityPermission extends BaseEntity {

    private String entityName;
    private boolean allowCreate = false;
    private boolean allowRead = false;
    private boolean allowUpdate = false;
    private boolean allowDelete = false;
    private boolean allowViewAll = false;
    private boolean modifyRequiresUserHasAccount = false;
    @ManyToOne
    @JsonIgnore
    @JoinColumn(name="appRole_fk")
    private AppRole role;

    public Set<String> convertToPermissionsList(){
        Set<String> permissions = new HashSet<>();
        if(allowCreate) permissions.add(entityName+":create");
        if(allowRead) permissions.add(entityName+":read");
        if(allowUpdate) permissions.add(entityName+":update");
        if(allowDelete) permissions.add(entityName+":delete");
        if(allowViewAll) permissions.add(entityName+":read:all");
        if(modifyRequiresUserHasAccount) permissions.add(entityName+":modifyWithAccount");
        return permissions;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public boolean isAllowCreate() {
        return allowCreate;
    }

    public void setAllowCreate(boolean allowCreate) {
        this.allowCreate = allowCreate;
    }

    public boolean isAllowRead() {
        return allowRead;
    }

    public void setAllowRead(boolean allowRead) {
        this.allowRead = allowRead;
    }

    public boolean isAllowUpdate() {
        return allowUpdate;
    }

    public void setAllowUpdate(boolean allowUpdate) {
        this.allowUpdate = allowUpdate;
    }

    public boolean isAllowDelete() {
        return allowDelete;
    }

    public void setAllowDelete(boolean allowDelete) {
        this.allowDelete = allowDelete;
    }

    public boolean isAllowViewAll() {
        return allowViewAll;
    }

    public void setAllowViewAll(boolean allowViewAll) {
        this.allowViewAll = allowViewAll;
    }

    public Boolean getModifyRequiresUserHasAccount() {
        return modifyRequiresUserHasAccount;
    }

    public void setModifyRequiresUserHasAccount(Boolean modifyRequiresUserHasAccount) {
        this.modifyRequiresUserHasAccount = modifyRequiresUserHasAccount;
    }

    public AppRole getRole() {
        return role;
    }

    public void setRole(AppRole role) {
        this.role = role;
    }
}
