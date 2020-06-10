package com.winllc.pki.ra.service.external.vendorimpl;

import com.winllc.pki.ra.service.external.IdentityProviderConnection;
import com.winllc.pki.ra.service.external.beans.IdentityExternal;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class KeycloakIdentityProviderConnection implements IdentityProviderConnection {

    @Autowired
    private Keycloak keycloak;
    @Value("${keycloak.admin-interface.realm}")
    private String realm;

    @Override
    public String getConnectionName() {
        return "identity-keycloak";
    }

    @Override
    public Optional<IdentityExternal> findByUid(String uid) {
        List<UserRepresentation> search = keycloak.realm(realm)
                .users().search(uid);


        return Optional.empty();
    }

    @Override
    public Optional<IdentityExternal> findByEmail(String email) {
        /*
        List<UserRepresentation> search = keycloak.realm(realm)
                .users().search("", "", "", email, 0, 1);

         */

        List<UserRepresentation> search = keycloak.realm(realm)
                .users().search(email);

        if(search.size() > 0){
            UserRepresentation representation = search.get(0);
            return Optional.of(representationToIdentity(representation));
        }

        return Optional.empty();
    }

    @Override
    public List<IdentityExternal> searchByNameLike(String search) {
        return null;
    }

    @Override
    public List<IdentityExternal> searchByEmailLike(String search) {
        return null;
    }

    private IdentityExternal representationToIdentity(UserRepresentation userRepresentation){
        IdentityExternal identity = new IdentityExternal();
        identity.setEmail(userRepresentation.getEmail());
        identity.setFirstName(userRepresentation.getFirstName());
        identity.setLastName(userRepresentation.getLastName());
        identity.setEnabled(userRepresentation.isEnabled());
        identity.setUid(userRepresentation.getUsername());
        return identity;
    }
}
