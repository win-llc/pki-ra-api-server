package com.winllc.pki.ra.beans.form;

import com.winllc.pki.ra.domain.ServerEntry;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.NotEmpty;
import java.util.List;

import static com.winllc.pki.ra.constants.ValidationRegex.FQDN_VALIDATION_PATTERN;

public class ServerEntryForm extends ValidForm<ServerEntry> {

    private String fqdn;
    private Long accountId;
    private List<String> alternateDnsValues;
    private String openidClientRedirectUrl;
    private Boolean allowPreAuthz;

    public ServerEntryForm(){}

    @Override
    protected void processIsValid() {
        if(!FQDN_VALIDATION_PATTERN.matcher(fqdn).matches()){
            getErrors().put("invalidFqdn", "Invalid fqdn: "+fqdn);
        }

        if(!CollectionUtils.isEmpty(alternateDnsValues)){
            for(String altDns : alternateDnsValues){
                if(!FQDN_VALIDATION_PATTERN.matcher(altDns).matches()){
                    getErrors().put("invalidAltDns", "Invalid Alt DNS: "+altDns);
                }
            }
        }
    }

    public ServerEntryForm(ServerEntry entry){
        super(entry);
        this.fqdn = entry.getFqdn();
        this.accountId = entry.getAccount().getId();
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

    public List<String> getAlternateDnsValues() {
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
