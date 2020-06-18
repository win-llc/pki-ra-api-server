package com.winllc.pki.ra.ca;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum CertAuthorityConnectionType {
    INTERNAL(new ArrayList<>()),
    DOGTAG(Stream.of(
            ConnectionProperty.build().addName("ADMIN_USERNAME")
            .addFriendlyName("Admin Username"),
            ConnectionProperty.build().addName("ADMIN_PASSWORD")
            .addFriendlyName("Admin Password")
            )
            .collect(Collectors.toList()));

    List<ConnectionProperty> requiredProperties;

    CertAuthorityConnectionType(List<ConnectionProperty> requiredProperties) {
        this.requiredProperties = requiredProperties;
    }

    public List<ConnectionProperty> getRequiredProperties() {
        return requiredProperties;
    }
}
