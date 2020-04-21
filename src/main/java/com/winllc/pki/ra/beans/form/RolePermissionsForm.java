package com.winllc.pki.ra.beans.form;

import javax.validation.constraints.NotEmpty;
import java.util.List;

public class RolePermissionsForm {
    @NotEmpty
    private String roleName;
    private List<String> permissions;

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}
