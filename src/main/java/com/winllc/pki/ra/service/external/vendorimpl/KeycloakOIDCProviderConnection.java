package com.winllc.pki.ra.service.external.vendorimpl;

import com.winllc.pki.ra.beans.OIDCClientDetails;
import com.winllc.pki.ra.domain.ServerEntry;
import com.winllc.pki.ra.exception.RAException;
import com.winllc.pki.ra.repository.ServerEntryRepository;
import com.winllc.pki.ra.service.external.OIDCProviderConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import java.util.*;

@Component
public class KeycloakOIDCProviderConnection implements OIDCProviderConnection {

    private static final Logger log = LogManager.getLogger(KeycloakOIDCProviderConnection.class);

    @Value("${keycloak.admin-interface.server-base-url}")
    private String serverBaseUrl;
    @Value("${keycloak.admin-interface.realm}")
    private String realm;
    @Value("${keycloak.admin-interface.custom-client-scope}")
    private String customClientScope;

    @Autowired
    private Keycloak keycloak;
    @Autowired
    private ServerEntryRepository serverEntryRepository;

    /*
    {
   "id":"3006acaf-7869-44bb-8841-737bb5964a29",
   "clientId":"example-test",
   "rootUrl":"http://localhost:3000",
   "adminUrl":"http://localhost:3000",
   "surrogateAuthRequired":false,
   "enabled":true,
   "clientAuthenticatorType":"client-secret",
   "redirectUris":[
      "http://localhost:3000/*"
   ],
   "webOrigins":[
      "http://localhost:3000"
   ],
   "notBefore":0,
   "bearerOnly":false,
   "consentRequired":false,
   "standardFlowEnabled":true,
   "implicitFlowEnabled":false,
   "directAccessGrantsEnabled":true,
   "serviceAccountsEnabled":false,
   "publicClient":false,
   "frontchannelLogout":false,
   "protocol":"openid-connect",
   "attributes":{
      "saml.server.signature":"false",
      "saml.server.signature.keyinfo.ext":"false",
      "saml.assertion.signature":"false",
      "saml.client.signature":"false",
      "saml.encrypt":"false",
      "saml.authnstatement":"false",
      "saml.onetimeuse.condition":"false",
      "saml_force_name_id_format":"false",
      "saml.multivalued.roles":"false",
      "saml.force.post.binding":"false",
      "exclude.session.state.from.auth.response":"false",
      "tls.client.certificate.bound.access.tokens":"false",
      "display.on.consent.screen":"false"
   },
   "authenticationFlowBindingOverrides":{

   },
   "fullScopeAllowed":true,
   "nodeReRegistrationTimeout":-1,
   "defaultClientScopes":[
      "web-origins",
      "role_list",
      "profile",
      "roles",
      "email"
   ],
   "optionalClientScopes":[
      "address",
      "phone",
      "offline_access",
      "microprofile-jwt"
   ],
   "access":{
      "view":true,
      "configure":true,
      "manage":true
   },
   "authorizationServicesEnabled":""
}
     */

    @Override
    public ServerEntry createClient(ServerEntry serverEntry) throws Exception {
        String url = "https://"+serverEntry.getFqdn();

        ClientRepresentation client = new ClientRepresentation();
        client.setId(UUID.randomUUID().toString());
        client.setClientId(serverEntry.getHostname());
        client.setRootUrl(url);
        client.setAdminUrl(url);
        client.setSurrogateAuthRequired(false);
        client.setEnabled(true);
        client.setClientAuthenticatorType("client-secret");
        client.setRedirectUris(Collections.singletonList(url+"/*"));
        client.setWebOrigins(Collections.singletonList(url));
        client.setNotBefore(0);
        client.setBearerOnly(false);
        client.setConsentRequired(false);
        client.setStandardFlowEnabled(true);
        client.setImplicitFlowEnabled(false);
        client.setDirectAccessGrantsEnabled(true);
        client.setServiceAccountsEnabled(false);
        client.setPublicClient(false);
        client.setFrontchannelLogout(false);
        client.setProtocol("openid-connect");

        Map<String, String> attributes = new HashMap<>();
        attributes.put("saml.server.signature", "false");
        attributes.put("saml.server.signature.keyinfo.ext", "false");
        attributes.put("saml.assertion.signature", "false");
        attributes.put("saml.client.signature", "false");
        attributes.put("saml.encrypt", "false");
        attributes.put("saml.authnstatement", "false");
        attributes.put("saml.onetimeuse.condition", "false");
        attributes.put("saml_force_name_id_format", "false");
        attributes.put("saml.multivalued.roles", "false");
        attributes.put("saml.force.post.binding", "false");
        attributes.put("exclude.session.state.from.auth.response", "false");
        attributes.put("tls.client.certificate.bound.access.tokens", "false");
        attributes.put("display.on.consent.screen", "false");

        client.setAttributes(attributes);
        client.setFullScopeAllowed(true);
        client.setNodeReRegistrationTimeout(-1);

        List<String> clientScopes = new ArrayList<>();
        clientScopes.add("web-origins");
        clientScopes.add("role_list");
        clientScopes.add("profile");
        clientScopes.add("roles");
        clientScopes.add("email");
        clientScopes.add(customClientScope);

        client.setDefaultClientScopes(clientScopes);

        List<String> optionalClientScopes = new ArrayList<>();
        optionalClientScopes.add("address");
        optionalClientScopes.add("phone");
        optionalClientScopes.add("offline_access");
        optionalClientScopes.add("microprofile-jwt");
        client.setOptionalClientScopes(optionalClientScopes);

        Map<String, Boolean> access = new HashMap<>();
        access.put("view", true);
        access.put("configure", true);
        access.put("manage", true);

        client.setAccess(access);

        Response response =
                keycloak.realm(realm)
                        .clients().create(client);

        int status = response.getStatus();

        if(status == 201){
            //Get the location of the crated client
            String location = response.getHeaderString("Location");
            String createdClientId = location.substring(location.lastIndexOf("/") + 1);

            Optional<ServerEntry> optionalServerEntry = serverEntryRepository.findById(serverEntry.getId());

            if(optionalServerEntry.isPresent()){
                serverEntry = optionalServerEntry.get();
                serverEntry.setOpenidClientId(createdClientId);

                serverEntry = serverEntryRepository.save(serverEntry);
                return serverEntry;
            }else{
                throw new RAException("Could not find Server Entry");
            }
        }else{
            if(status == 409){
                throw new RAException("Client already exists: "+serverEntry.getFqdn());
            }
            throw new RAException("Client not created: "+ response.getStatus() + ", body: "+response.getEntity());
        }
    }

    @Override
    public ServerEntry deleteClient(ServerEntry serverEntry) throws RAException {
        log.info("Going to delete Keycloak Client: "+serverEntry);
        Optional<ServerEntry> optionalServerEntry = serverEntryRepository.findById(serverEntry.getId());

        if(optionalServerEntry.isPresent()) {
            serverEntry = optionalServerEntry.get();

            keycloak.realm(realm)
                    .clients().get(serverEntry.getOpenidClientId())
                    .remove();

            //if successful, reset openid client on server entry
            serverEntry.setOpenidClientId(null);
            serverEntry = serverEntryRepository.save(serverEntry);

            return serverEntry;
        }else{
            throw new RAException("Could not find server entry, should not be possible");
        }
    }

    @Override
    public OIDCClientDetails getClient(ServerEntry serverEntry) {
        log.info("Getting client for: "+serverEntry.getFqdn());
        String oidcProviderMetadataURL = serverBaseUrl+"/realms/"+realm+"/.well-known/openid-configuration";

        OIDCClientDetails clientDetails = new OIDCClientDetails();
        clientDetails.setOidcProviderMetadataUrlValue(oidcProviderMetadataURL);
        clientDetails.setOidcRedirectUriValue("https://"+serverEntry.getFqdn()+"/*");

        ClientResource clientResource = keycloak.realm(realm)
                .clients().get(serverEntry.getOpenidClientId());

        ClientRepresentation clientRepresentation = clientResource.toRepresentation();
        clientDetails.setOidcClientIdValue(clientRepresentation.getClientId());

        CredentialRepresentation secret = clientResource.getSecret();
        clientDetails.setOidcSecretValue(secret.getValue());

        return clientDetails;
    }

    @Override
    public String getConnectionName() {
        return "oidc-keycloak";
    }
}
