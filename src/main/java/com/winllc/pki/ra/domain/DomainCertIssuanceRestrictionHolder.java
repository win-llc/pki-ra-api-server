package com.winllc.pki.ra.domain;

import java.util.Set;

public interface DomainCertIssuanceRestrictionHolder {
    Set<DomainPolicy> getDomainIssuanceRestrictions();
}
