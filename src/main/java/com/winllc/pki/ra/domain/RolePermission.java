package com.winllc.pki.ra.domain;

import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "role_permission")
public class RolePermission extends AbstractPersistable<Long> {

    @Column(nullable = false)
    private String roleName;
    private String permission = "";

    public RolePermission(String roleName, String permission){
        this.roleName = roleName;
        this.permission = permission;
    }

    public RolePermission(){}

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RolePermission that = (RolePermission) o;
        return Objects.equals(roleName, that.roleName) &&
                Objects.equals(permission, that.permission);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), roleName, permission);
    }
}
