package com.winllc.pki.ra.beans.form;

import com.winllc.acme.common.domain.ServerEntry;
import com.winllc.pki.ra.util.FormValidationUtil;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ServerEntryForm extends ValidForm<ServerEntry> {

    private String fqdn;
    private Long accountId;
    private String projectName;
    private Long domainId;
    private List<String> alternateDnsValues = new ArrayList<>();
    private String openidClientRedirectUrl;
    private Boolean allowPreAuthz = false;

    public ServerEntryForm(){}

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



}
