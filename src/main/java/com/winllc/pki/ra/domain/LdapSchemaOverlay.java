package com.winllc.pki.ra.domain;

import com.winllc.acme.common.domain.BaseEntity;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "ldap_schema_overlay")
public class LdapSchemaOverlay extends BaseEntity {

    @Column(nullable = false)
    private String ldapObjectType;
    //@JsonIgnore
    @OneToMany(mappedBy = "ldapSchemaOverlay", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private Set<LdapSchemaOverlayAttribute> attributeMap;

    @PreRemove
    public void preRemove(){
        if(getAttributeMap() != null){
            for(LdapSchemaOverlayAttribute attribute : getAttributeMap()){
                attribute.setLdapSchemaOverlay(null);
            }
        }
    }

    public String getLdapObjectType() {
        return ldapObjectType;
    }

    public void setLdapObjectType(String ldapObjectType) {
        this.ldapObjectType = ldapObjectType;
    }

    public Set<LdapSchemaOverlayAttribute> getAttributeMap() {
        if(attributeMap == null) attributeMap = new HashSet<>();
        return attributeMap;
    }

    public void setAttributeMap(Set<LdapSchemaOverlayAttribute> attributeMap) {
        this.attributeMap = attributeMap;
    }
}
