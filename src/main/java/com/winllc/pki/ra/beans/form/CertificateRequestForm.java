package com.winllc.pki.ra.beans.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.acme.common.domain.CertificateRequest;
import com.winllc.pki.ra.util.FormValidationUtil;
import com.winllc.ra.integration.ca.SubjectAltName;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;


public class CertificateRequestForm extends ValidForm<CertificateRequest> {

    private String csr;
    private String name;
    private String certAuthorityName;
    @NotNull
    private Long accountId;
    private List<SubjectAltName> requestedDnsNames;
    private String primaryDnsHostname;
    private Long primaryDnsDomainId;

    public String getCsr() {
        return csr;
    }

    public void setCsr(String csr) {
        this.csr = csr;
    }

    public String getPrimaryDnsHostname() {
        return primaryDnsHostname;
    }

    public void setPrimaryDnsHostname(String primaryDnsHostname) {
        this.primaryDnsHostname = primaryDnsHostname;
    }

    public Long getPrimaryDnsDomainId() {
        return primaryDnsDomainId;
    }

    public void setPrimaryDnsDomainId(Long primaryDnsDomainId) {
        this.primaryDnsDomainId = primaryDnsDomainId;
    }

    public List<SubjectAltName> getRequestedDnsNames() {
        return requestedDnsNames;
    }

    public void setRequestedDnsNames(List<SubjectAltName> requestedDnsNames) {
        this.requestedDnsNames = requestedDnsNames;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCertAuthorityName() {
        return certAuthorityName;
    }

    public void setCertAuthorityName(String certAuthorityName) {
        this.certAuthorityName = certAuthorityName;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }


    @JsonIgnore
    @Override
    protected void processIsValid() {
        //validate CSR
        try {
            CertUtil.convertPemToPKCS10CertificationRequest(this.csr);
        }catch (Exception e){
            getErrors().put("invalidCsr", e.getMessage());
        }

        //Validate fqdns
        if(!CollectionUtils.isEmpty(requestedDnsNames)){
            List<String> errorFqdns = new ArrayList<>();
            for(SubjectAltName san : requestedDnsNames){
                if(!FormValidationUtil.isValidFqdn(san.getValue())){
                    errorFqdns.add(san.getValue());
                }
            }
            if(errorFqdns.size() > 0){
                getErrors().put("invalidFqdn", String.join(", ", errorFqdns));
            }
        }
    }
}
