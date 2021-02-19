package com.winllc.pki.ra.beans.info;

import com.winllc.pki.ra.domain.AppRole;
import com.winllc.pki.ra.domain.EntityPermission;
import com.winllc.pki.ra.domain.RolePermission;
import org.hibernate.Hibernate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppRoleInfo {

    private Long id;
    private String name;
    private Set<EntityPermission> permissions;
    private Set<RolePermission> additionalPermissions;

    public AppRoleInfo(AppRole appRole){
        this.id = appRole.getId();
        this.name = appRole.getName();
        Hibernate.initialize(appRole.getPermissions());
        Hibernate.initialize(appRole.getAdditionalPermissions());
        this.permissions = appRole.getPermissions();
        this.additionalPermissions = appRole.getAdditionalPermissions();
    }

    private AppRoleInfo(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<EntityPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<EntityPermission> permissions) {
        this.permissions = permissions;
    }

    public Set<RolePermission> getAdditionalPermissions() {
        if(additionalPermissions == null) additionalPermissions = new HashSet<>();
        return additionalPermissions;
    }

    public void setAdditionalPermissions(Set<RolePermission> additionalPermissions) {
        this.additionalPermissions = additionalPermissions;
    }
}
