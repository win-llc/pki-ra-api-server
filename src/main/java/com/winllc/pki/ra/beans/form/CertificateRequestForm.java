package com.winllc.pki.ra.beans.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.winllc.acme.common.SubjectAltName;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.domain.CertificateRequest;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.winllc.pki.ra.constants.ValidationRegex.FQDN_VALIDATION_REGEX;

public class CertificateRequestForm extends ValidForm<CertificateRequest> {

    private String csr;
    private String name;
    private String certAuthorityName;
    @NotNull
    private Long accountId;
    private List<SubjectAltName> requestedDnsNames;

    public String getCsr() {
        return csr;
    }

    public void setCsr(String csr) {
        this.csr = csr;
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
            Pattern fqdnPattern = Pattern.compile(FQDN_VALIDATION_REGEX);
            List<String> errorFqdns = new ArrayList<>();
            for(SubjectAltName san : requestedDnsNames){
                if(!fqdnPattern.matcher(san.getValue()).matches()){
                    errorFqdns.add(san.getValue());
                }
            }
            if(errorFqdns.size() > 0){
                getErrors().put("invalidFqdn", String.join(", ", errorFqdns));
            }
        }
    }
}
