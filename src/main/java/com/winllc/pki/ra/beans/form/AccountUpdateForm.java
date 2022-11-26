package com.winllc.pki.ra.beans.form;

import com.winllc.pki.ra.beans.PocFormEntry;
import com.winllc.acme.common.domain.Account;
import com.winllc.pki.ra.beans.info.DomainInfo;
import com.winllc.pki.ra.beans.info.UserInfo;
import com.winllc.pki.ra.util.FormValidationUtil;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.Email;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class AccountUpdateForm extends ValidForm<Account> {

    @Email
    private String accountOwnerEmail;
    private List<PocFormEntry> pocEmails;
    private String securityPolicyProjectId;
    private String entityBaseDn;

    private String keyIdentifier;
    private String macKey;
    private String macKeyBase64;
    private String projectName;
    private boolean acmeRequireHttpValidation;
    private boolean enabled;
    private String securityPolicyServerProjectId;
    private String creationDate;

    private UserInfo accountOwner;
    private boolean userIsOwner = false;
    private List<PocFormEntry> pocs;
    private List<DomainInfo> canIssueDomains;

    public AccountUpdateForm(Account entity) {
        super(entity);
        this.entityBaseDn = entity.getEntityBaseDn();
        this.projectName = entity.getProjectName();
        this.creationDate = entity.getCreationDate().toString();
        setSecurityPolicyServerProjectId(entity.getSecurityPolicyServerProjectId());
        setSecurityPolicyProjectId(entity.getSecurityPolicyServerProjectId());
    }

    public AccountUpdateForm(){}


    @Override
    protected void processIsValid() {
        if(!CollectionUtils.isEmpty(pocEmails)){
            List<String> invalidEmails = pocEmails.stream()
                    .filter(p -> !FormValidationUtil.isValidEmailAddress(p.getEmail()))
                    .map(p -> p.getEmail())
                    .collect(Collectors.toList());
            if(invalidEmails.size() > 0){
                errors.put("pocEmails", "Invalid emails: "+String.join(", ", invalidEmails));
            }
        }
    }

    public List<PocFormEntry> getPocEmails() {
        return pocEmails;
    }

    public void setPocEmails(List<PocFormEntry> pocEmails) {
        this.pocEmails = pocEmails;
    }

    public String getEntityBaseDn() {
        return entityBaseDn;
    }

    public void setEntityBaseDn(String entityBaseDn) {
        this.entityBaseDn = entityBaseDn;
    }
}
