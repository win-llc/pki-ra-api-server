package com.winllc.pki.ra.service.external;

import com.winllc.pki.ra.beans.OIDCClientDetails;
import com.winllc.acme.common.domain.ServerEntry;
import com.winllc.pki.ra.exception.RAException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OIDCProviderService {

    @Value("${external-services.oidc.enabled-connection-names}")
    private List<String> enabledConnections;
    @Autowired
    private List<OIDCProviderConnection> connections;

    public ServerEntry createClient(String connectionName, ServerEntry serverEntry) throws Exception {
        OIDCProviderConnection connection = getConnection(connectionName);
        return connection.createClient(serverEntry);
    }

    public ServerEntry deleteClient(String connectionName, ServerEntry serverEntry) throws Exception {
        OIDCProviderConnection connection = getConnection(connectionName);
        return connection.deleteClient(serverEntry);
    }

    public OIDCClientDetails getClient(String connectionName, ServerEntry serverEntry) throws Exception {
        OIDCProviderConnection connection = getConnection(connectionName);
        return connection.getClient(serverEntry);
    }

    private OIDCProviderConnection getConnection(String name) throws Exception {
        for(OIDCProviderConnection connection : connections){
            //todo filter non-enabled connections
            if(connection.getConnectionName().equalsIgnoreCase(name)) return connection;
        }

        throw new Exception("Connection not found: "+name);
    }
}
