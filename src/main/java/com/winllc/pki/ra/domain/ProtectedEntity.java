package com.winllc.pki.ra.domain;

import net.minidev.json.annotate.JsonIgnore;

public interface ProtectedEntity {
    @JsonIgnore
    String getProtectedEntityName();
}
