package com.winllc.pki.ra.beans.form;

import com.winllc.pki.ra.domain.DomainLinkToAccountRequest;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class DomainLinkToAccountRequestForm extends ValidForm<DomainLinkToAccountRequest> {

    @NotNull
    private Long accountId;
    private List<Long> requestedDomainIds;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public List<Long> getRequestedDomainIds() {
        return requestedDomainIds;
    }

    public void setRequestedDomainIds(List<Long> requestedDomainIds) {
        this.requestedDomainIds = requestedDomainIds;
    }


    @Override
    protected void processIsValid() {
        if(!CollectionUtils.isEmpty(requestedDomainIds)){
            if(requestedDomainIds.stream().anyMatch(Objects::isNull)){
                getErrors().put("requestedDomainIds", "Contains a null field, not valid");
            }
        }else{
            getErrors().put("requestedDomainIds", "Domain IDs can't be empty");
        }
    }
}
