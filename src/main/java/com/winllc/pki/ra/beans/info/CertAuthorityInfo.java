package com.winllc.pki.ra.beans.info;

import com.winllc.pki.ra.beans.form.ValidForm;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CertAuthorityInfo extends ValidForm {

    private String dn;
    private String name;
    private String validFrom;
    private String validTo;
    private String trustChain;
    private String latestCrl;

    public CertAuthorityInfo() {
    }

    @Override
    protected void processIsValid() {

    }
}
