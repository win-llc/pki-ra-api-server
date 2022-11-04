package com.winllc.pki.ra.beans.form;

import com.winllc.acme.common.domain.EstServerProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class EstServerPropertiesForm extends ValidForm<EstServerProperties> {

    private String name;
    private String caConnectionName;
    private Date creationDate;

    public EstServerPropertiesForm(EstServerProperties entity) {
        super(entity);
        this.name = entity.getName();
        this.caConnectionName = entity.getCaConnectionName();
        this.creationDate = entity.getCreationDate();
    }

    public EstServerPropertiesForm() {
    }

    @Override
    protected void processIsValid() {

    }
}
