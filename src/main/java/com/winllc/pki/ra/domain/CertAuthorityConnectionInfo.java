package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.winllc.pki.ra.ca.CertAuthorityConnectionType;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.util.CollectionUtils;

import javax.persistence.*;
import java.util.Optional;
import java.util.Set;

@Entity
public class CertAuthorityConnectionInfo extends AbstractPersistable<Long> {

    @Column(unique = true)
    private String name;
    @Column(nullable = false)
    private CertAuthorityConnectionType type;
    private String baseUrl;
    private String issuePath;
    private String revokePath;
    private String searchPath;
    @JsonIgnore
    @OneToMany(mappedBy = "certAuthorityConnectionInfo", fetch = FetchType.EAGER)
    private Set<CertAuthorityConnectionProperty> properties;

    @PreRemove
    private void preRemove(){
        Set<CertAuthorityConnectionProperty> properties = getProperties();
        if(!CollectionUtils.isEmpty(properties)){
            for(CertAuthorityConnectionProperty property : properties){
                property.setCertAuthorityConnectionInfo(null);
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CertAuthorityConnectionType getType() {
        return type;
    }

    public void setType(CertAuthorityConnectionType type) {
        this.type = type;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getIssuePath() {
        return issuePath;
    }

    public void setIssuePath(String issuePath) {
        this.issuePath = issuePath;
    }

    public String getRevokePath() {
        return revokePath;
    }

    public void setRevokePath(String revokePath) {
        this.revokePath = revokePath;
    }

    public String getSearchPath() {
        return searchPath;
    }

    public void setSearchPath(String searchPath) {
        this.searchPath = searchPath;
    }

    public Set<CertAuthorityConnectionProperty> getProperties() {
        return properties;
    }

    public void setProperties(Set<CertAuthorityConnectionProperty> properties) {
        this.properties = properties;
    }

    public Optional<CertAuthorityConnectionProperty> getPropertyByName(String name){
        if(getProperties() != null){
            return getProperties().stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst();
        }
        return Optional.empty();
    }
}
