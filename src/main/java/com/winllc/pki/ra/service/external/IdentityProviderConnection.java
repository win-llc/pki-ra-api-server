package com.winllc.pki.ra.service.external;

import com.winllc.pki.ra.service.external.beans.IdentityExternal;

import java.util.List;
import java.util.Optional;

public interface IdentityProviderConnection extends ExternalServiceConnection  {
    Optional<IdentityExternal> findByUid(String uid);
    Optional<IdentityExternal> findByEmail(String email);
    List<IdentityExternal> searchByNameLike(String search);
    List<IdentityExternal> searchByEmailLike(String search);
}
