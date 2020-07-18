package com.winllc.pki.ra.service.external;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public class SecurityPolicyServerProjectDetails implements Serializable {
    private String projectId;
    private String projectName;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Set<String> fqdns;

    //todo other security attributes associated with a project


    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Set<String> getFqdns() {
        return fqdns;
    }

    public void setFqdns(Set<String> fqdns) {
        this.fqdns = fqdns;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }
}
