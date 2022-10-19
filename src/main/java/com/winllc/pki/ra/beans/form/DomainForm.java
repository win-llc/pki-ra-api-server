package com.winllc.pki.ra.beans.form;

import com.winllc.acme.common.domain.Domain;
import com.winllc.pki.ra.util.FormValidationUtil;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import java.util.regex.Pattern;

@Getter
@Setter
public class DomainForm extends ValidForm<Domain> {

    @NotEmpty
    private String base;
    private String fullDomainName;
    private Long parentDomainId;
    private String parentDomainBase;

    public DomainForm(Domain entity) {
        super(entity);
        this.base = entity.getBase();
        this.fullDomainName = entity.getFullDomainName();

        if(entity.getParentDomain() != null){
            this.parentDomainId = entity.getParentDomain().getId();
            this.parentDomainBase = entity.getParentDomain().getFullDomainName();
        }
    }

    public DomainForm(String base) {
        this.base = base;
    }

    private DomainForm(){}

    @Override
    protected void processIsValid() {
        if(!FormValidationUtil.isValidFqdn(base)){
            getErrors().put("invalidBase", "Invalid base");
        }
    }


}
