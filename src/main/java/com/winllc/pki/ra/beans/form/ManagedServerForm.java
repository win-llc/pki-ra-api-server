package com.winllc.pki.ra.beans.form;

import com.winllc.acme.common.domain.ManagedServer;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import java.time.LocalDateTime;

@Getter
@Setter
public class ManagedServerForm extends ValidForm<ManagedServer> {

    private String uniqueId;
    private String fqdn;
    private String serverEntryId;
    private String domain;
    private String project;
    private String projectId;
    private LocalDateTime latestCertIssuedOn;
    private LocalDateTime latestCertExpiresOn;
    private String latestCertIssuer;
    private String latestCertSerial;
    private String latestCertSubject;

    public ManagedServerForm(ManagedServer entity) {
        super(entity);

        setUniqueId(entity.getUniqueId());
        setFqdn(entity.getFqdn());
        setServerEntryId(entity.getServerEntryId());
        setDomain(entity.getDomain());
        setProject(entity.getProject());
        setProjectId(entity.getProjectId());
        setLatestCertIssuedOn(entity.getLatestCertIssuedOn());
        setLatestCertExpiresOn(entity.getLatestCertExpiresOn());
        setLatestCertIssuer(entity.getLatestCertIssuer());
        setLatestCertSerial(entity.getLatestCertSerial());
        setLatestCertSubject(entity.getLatestCertSubject());
    }

    @Override
    protected void processIsValid() {

    }
}
