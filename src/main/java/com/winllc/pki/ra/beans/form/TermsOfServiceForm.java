package com.winllc.pki.ra.beans.form;

import com.winllc.acme.common.domain.TermsOfService;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
public class TermsOfServiceForm  extends ValidForm<TermsOfService> {
    private String text;
    private String forDirectoryName;

    public TermsOfServiceForm(){}

    public TermsOfServiceForm(TermsOfService termsOfService){
        super(termsOfService);
        this.text = termsOfService.getText();
        this.forDirectoryName = termsOfService.getForDirectoryName();
    }

    @Override
    protected void processIsValid() {

    }
}
