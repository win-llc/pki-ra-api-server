package com.winllc.pki.ra.ca;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum CertAuthorityConnectionType {
    INTERNAL(new ArrayList<>()),
    DOGTAG(Stream.of("ADMIN_USERNAME", "ADMIN_PASSWORD").collect(Collectors.toList()));

    List<String> requiredProperties;

    CertAuthorityConnectionType(List<String> requiredProperties) {
        this.requiredProperties = requiredProperties;
    }

    public List<String> getRequiredProperties() {
        return requiredProperties;
    }
}
