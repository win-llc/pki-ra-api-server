package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "app_role")
public class AppRole extends AbstractPersistable<Long> {

    private String name;
    @JsonIgnore
    @OneToMany(mappedBy = "role", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private Set<EntityPermission> permissions;

    @JsonIgnore
    @OneToMany(mappedBy = "role", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private Set<RolePermission> additionalPermissions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<EntityPermission> getPermissions() {
        if(permissions == null) permissions = new HashSet<>();
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
