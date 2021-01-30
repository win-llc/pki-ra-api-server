package com.winllc.pki.ra.beans.info;

import java.util.List;

public class AccountInfoItem {
    private String projectName;
    private List<String> pocs;
    private List<String> domains;
    private String validTo;
    private Boolean enabled;

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public List<String> getPocs() {
        return pocs;
    }

    public void setPocs(List<String> pocs) {
        this.pocs = pocs;
    }

    public List<String> getDomains() {
        return domains;
    }

    public void setDomains(List<String> domains) {
        this.domains = domains;
    }

    public String getValidTo() {
        return validTo;
    }

    public void setValidTo(String validTo) {
        this.validTo = validTo;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
