package com.winllc.pki.ra.beans.form;

import com.winllc.acme.common.domain.LdapSchemaOverlay;
import com.winllc.acme.common.domain.LdapSchemaOverlayAttribute;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.util.Set;

@Getter
@Setter
public class LdapSchemaOverlayForm extends ValidForm<LdapSchemaOverlay> {

    private String ldapObjectType;
    private Set<LdapSchemaOverlayAttribute> attributeMap;

    public LdapSchemaOverlayForm(LdapSchemaOverlay entity) {
        super(entity);
        this.ldapObjectType = entity.getLdapObjectType();
        Hibernate.initialize(entity.getAttributeMap());
        this.attributeMap = entity.getAttributeMap();
    }

    public LdapSchemaOverlayForm() {
        super();
    }

    @Override
    protected void processIsValid() {

    }
}
