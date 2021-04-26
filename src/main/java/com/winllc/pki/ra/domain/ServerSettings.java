package com.winllc.pki.ra.domain;

import com.winllc.acme.common.domain.BaseEntity;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class ServerSettings extends BaseEntity {

    @Column(unique = true)
    private String property;
    private String value = "";
    private String groupName;

    public ServerSettings(){}

    public ServerSettings(String property){
        this.property = property;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof ServerSettings)) return false;

        ServerSettings that = (ServerSettings) o;

        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(getProperty(), that.getProperty())
                .append(getGroupName(), that.getGroupName())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .appendSuper(super.hashCode())
                .append(getProperty())
                .append(getGroupName())
                .toHashCode();
    }
}
