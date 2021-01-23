package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;

@Entity
@Table(name = "ldap_schema_overlay_attribute")
public class LdapSchemaOverlayAttribute extends AbstractPersistable<Long> {

    private String name;
    private Boolean enabled;
    @ManyToOne
    @JsonIgnore
    @JoinColumn(name="ldapSchemaOverlay_fk")
    private LdapSchemaOverlay ldapSchemaOverlay;

    @PreRemove
    public void preRemove(){
        if(ldapSchemaOverlay != null){
            ldapSchemaOverlay.getAttributeMap().remove(this);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LdapSchemaOverlay getLdapSchemaOverlay() {
        return ldapSchemaOverlay;
    }

    public void setLdapSchemaOverlay(LdapSchemaOverlay ldapSchemaOverlay) {
        this.ldapSchemaOverlay = ldapSchemaOverlay;
    }
}
