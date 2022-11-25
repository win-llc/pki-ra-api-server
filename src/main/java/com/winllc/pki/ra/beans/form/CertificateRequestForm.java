package com.winllc.pki.ra.beans.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.acme.common.domain.CertificateRequest;
import com.winllc.pki.ra.util.FormValidationUtil;
import com.winllc.ra.integration.ca.SubjectAltName;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
public class CertificateRequestForm extends ValidForm<CertificateRequest> {

    private String csr;
    private String name;
    private String status;

    private String submittedOn;
    private String certAuthorityName;
    @NotNull
    private Long accountId;
    private List<SubjectAltName> requestedDnsNames;
    private String primaryDnsHostname;
    private Long primaryDnsDomainId;

    public CertificateRequestForm(CertificateRequest entity) {
        super(entity);
        this.csr = entity.getCsr();
        this.certAuthorityName = entity.getCertAuthorityName();
        this.status = entity.getStatus();
        if(entity.getSubmittedOn() != null) {
            this.submittedOn = entity.getSubmittedOn().toString();
        }
        if(entity.getAccount() != null) {
            this.accountId = entity.getAccount().getId();
        }
        //todo
        //this.requestedDnsNames = entity.getRequestedDnsNames();
    }

    public CertificateRequestForm() {
        super();
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
