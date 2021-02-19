package com.winllc.pki.ra.beans.form;

import com.winllc.pki.ra.beans.info.AppRoleInfo;

import java.util.List;

public class AppRolePermissionsForm {

    private List<AppRoleInfo> roles;

    public List<AppRoleInfo> getRoles() {
        return roles;
    }

    public void setRoles(List<AppRoleInfo> roles) {
        this.roles = roles;
    }

}
