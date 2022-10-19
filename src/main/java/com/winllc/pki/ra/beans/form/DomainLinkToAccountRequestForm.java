package com.winllc.pki.ra.beans.form;

import com.winllc.acme.common.domain.DomainLinkToAccountRequest;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter
@Setter
public class DomainLinkToAccountRequestForm extends ValidForm<DomainLinkToAccountRequest> {

    @NotNull
    private Long accountId;
    private List<Long> requestedDomainIds;

    public DomainLinkToAccountRequestForm(DomainLinkToAccountRequest entity) {
        super(entity);
        this.accountId = entity.getAccountId();
        this.requestedDomainIds = new ArrayList<>(entity.getRequestedDomainIds());
    }

    public DomainLinkToAccountRequestForm() {
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
