package com.winllc.pki.ra.service.external;

import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.service.external.beans.IdentityExternal;
import com.winllc.pki.ra.service.external.vendorimpl.KeycloakIdentityProviderConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class IdentityProviderService {

    @Value("${external-services.identity.enabled-connection-names}")
    private List<String> enabledConnections;
    //todo make generic

    //private Map<String, IdentityProviderConnection> idpConnectionMap = new HashMap<>();

    @Autowired
    private List<IdentityProviderConnection> identityProviderConnections;

    private IdentityProviderConnection loadIdentityProvider(String name) throws RAObjectNotFoundException {
        for(IdentityProviderConnection connection : identityProviderConnections){
            if(connection.getConnectionName().equalsIgnoreCase(name)) return connection;
        }
        throw new RAObjectNotFoundException(IdentityProviderConnection.class, name);
    }

    public Optional<IdentityExternal> findByUid(String uid) {
        return Optional.empty();
    }

    public Optional<IdentityExternal> findByEmail(String connectionName, String email) throws RAObjectNotFoundException {
        return loadIdentityProvider(connectionName).findByEmail(email);
    }

    public List<IdentityExternal> searchByNameLike(String search) {
        return null;
    }

    public List<IdentityExternal> searchByEmailLike(String search) {
        return null;
    }
}
