package com.winllc.pki.ra.beans.form;

import com.winllc.acme.common.domain.ServerEntry;
import com.winllc.pki.ra.util.FormValidationUtil;
import org.hibernate.Hibernate;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

public class ServerEntryForm extends ValidForm<ServerEntry> {

    private String fqdn;
    private Long accountId;
    private Long domainId;
    private List<String> alternateDnsValues;
    private String openidClientRedirectUrl;
    private Boolean allowPreAuthz;

    public ServerEntryForm(){}

    @Override
    protected void processIsValid() {

        if(!FormValidationUtil.isValidFqdn(fqdn)){
            getErrors().put("invalidFqdn", "Invalid fqdn: "+fqdn);
        }

        if(!CollectionUtils.isEmpty(alternateDnsValues)){
            for(String altDns : alternateDnsValues){
                if(!FormValidationUtil.isValidFqdn(altDns)){
                    getErrors().put("invalidAltDns", "Invalid Alt DNS: "+altDns);
                }
            }
        }
    }

    public ServerEntryForm(ServerEntry entry){
        super(entry);
        this.fqdn = entry.getFqdn();
        //Hibernate.initialize(entry.getAccount());
        //this.accountId = entry.getAccount().getId();
        //todo add domain id
        this.alternateDnsValues = entry.getAlternateDnsValues();
        this.openidClientRedirectUrl = entry.getOpenidClientRedirectUrl();
        this.allowPreAuthz = entry.getAcmeAllowPreAuthz();
    }

    public String getFqdn() {
        return fqdn;
    }

    public void setFqdn(String fqdn) {
        this.fqdn = fqdn;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public List<String> getAlternateDnsValues() {
        if(alternateDnsValues == null) alternateDnsValues = new ArrayList<>();
        return alternateDnsValues;
    }

    public void setAlternateDnsValues(List<String> alternateDnsValues) {
        this.alternateDnsValues = alternateDnsValues;
    }

    public String getOpenidClientRedirectUrl() {
        return openidClientRedirectUrl;
    }

    public void setOpenidClientRedirectUrl(String openidClientRedirectUrl) {
        this.openidClientRedirectUrl = openidClientRedirectUrl;
    }

    public Boolean getAllowPreAuthz() {
        return allowPreAuthz;
    }

    public void setAllowPreAuthz(Boolean allowPreAuthz) {
        this.allowPreAuthz = allowPreAuthz;
    }
}
