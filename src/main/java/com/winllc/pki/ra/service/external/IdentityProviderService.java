package com.winllc.pki.ra.service.external;

import com.winllc.pki.ra.service.external.beans.IdentityExternal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class IdentityProviderService {

    //todo make generic
    //private Map<String, IdentityProviderConnection> idpConnectionMap = new HashMap<>();
    @Autowired
    private KeycloakIdentityProviderConnection identityProviderConnection;

    private void loadIdentityProvider(){

    }

    public Optional<IdentityExternal> findByUid(String uid) {
        return Optional.empty();
    }

    public Optional<IdentityExternal> findByEmail(String email) {
        return identityProviderConnection.findByEmail(email);
    }

    public List<IdentityExternal> searchByNameLike(String search) {
        return null;
    }

    public List<IdentityExternal> searchByEmailLike(String search) {
        return null;
    }
}
