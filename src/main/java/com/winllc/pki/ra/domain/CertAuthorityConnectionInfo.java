package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.winllc.pki.ra.ca.CertAuthorityConnectionType;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.util.CollectionUtils;

import javax.persistence.*;
import java.util.HashSet;
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
    //todo require this
    @Column(length = 3000)
    private String trustChainBase64;
    //use to pull auth cert from application keystore for mutual client auth
    private String authKeyAlias;

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
        if(properties == null) properties = new HashSet<>();
        return properties;
    }

    public void setProperties(Set<CertAuthorityConnectionProperty> properties) {
        this.properties = properties;
    }

    public String getTrustChainBase64() {
        return trustChainBase64;
    }

    public void setTrustChainBase64(String trustChainBase64) {
        this.trustChainBase64 = trustChainBase64;
    }

    public String getAuthKeyAlias() {
        return authKeyAlias;
    }

    public void setAuthKeyAlias(String authKeyAlias) {
        this.authKeyAlias = authKeyAlias;
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
