package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.form.UniqueEntityLookupForm;
import com.winllc.acme.common.domain.AuthCredential;
import com.winllc.acme.common.domain.ServerEntry;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/buildCommand")
public class CommandBuilderService {

    @Value("${win-ra.acme-server-public-url}")
    private String acmeBaseUrl;
    @Value("${win-ra.est-server-url}")
    private String estBaseUrl;
    private final ServerEntryService serverEntryService;
    private final AuthCredentialService authCredentialService;

    public CommandBuilderService(ServerEntryService serverEntryService, AuthCredentialService authCredentialService) {
        this.serverEntryService = serverEntryService;
        this.authCredentialService = authCredentialService;
    }

    @GetMapping("/{command}/server/{id}/{applicationName}")
    public List<String> buildCertbotCommand(@PathVariable String command, @PathVariable Long id, @PathVariable String applicationName,
                                            Authentication authentication)
            throws Exception {
        ServerEntry serverEntry = serverEntryService.getServerEntry(id);

        List<String> commands = new LinkedList<>();

        switch (command) {
            case "certbot":
                String registerCommand = buildCertbotRegisterCommand(serverEntry, authentication.getName());
                String certCommand = buildCertbotCertCommand(serverEntry, applicationName, authentication.getName());
                commands.add(registerCommand);
                commands.add(certCommand);
                commands.add(buildCertbotRenewalCron());
                break;
            case "win-acme":
                commands.add(buildWinAcmeRequiredInputs(serverEntry, applicationName));
                break;
            case "est":
                commands.addAll(buildEstRequiredInputs(serverEntry));
                break;
        }

        return commands;
    }

    private String buildCertbotRegisterCommand(ServerEntry serverEntry, String email) throws Exception {

        Optional<AuthCredential> optionalAuthCredential = getAuthCredential(serverEntry);

        if (optionalAuthCredential.isPresent()) {
            AuthCredential credential = optionalAuthCredential.get();

            //certbot register --no-eff-email --server https://winra.winllc-dev.com/acme/acme/directory
            // --agree-tos -m test@test.com -q --eab-kid AJhLkhbxTNDBAH9BOTlb
            // --eab-hmac-key ODhOUVdEZmxQQlhLOG8wb1pOQTZJQnBub2Qwa3VhVFhmZHk4TjNySThOOXN1bXEwdjZPSjMyaWd3

            List<String> commandParts = new LinkedList<>();
            commandParts.add("certbot");
            commandParts.add("register");
            commandParts.add("--no-eff-email");
            commandParts.add("--no-verify-ssl");
            commandParts.add("--agree-tos");
            commandParts.add("-q");
            commandParts.add("-m");
            commandParts.add(email);
            commandParts.add("--server");
            commandParts.add(acmeBaseUrl + "/acme/directory");
            commandParts.add("--eab-kid");
            commandParts.add(credential.getKeyIdentifier());
            commandParts.add("--eab-hmac-key");
            commandParts.add(credential.getMacKeyBase64());

            return String.join(" ", commandParts);
        } else {
            throw new RAObjectNotFoundException(AuthCredential.class, serverEntry.getFqdn());
        }
    }

    private String buildCertbotCertCommand(ServerEntry serverEntry, String applicationName, String email) {
        List<String> commandParts = new LinkedList<>();
        commandParts.add("certbot");
        if (applicationName.contentEquals("standalone") || applicationName.contentEquals("webroot")) {
            commandParts.add("certonly");
        }
        commandParts.add("--" + applicationName);
        commandParts.add("--no-verify-ssl");
        commandParts.add("--server");
        //todo dynamic
        commandParts.add(acmeBaseUrl + "/acme/directory");
        commandParts.add("-d");
        commandParts.add(serverEntry.getFqdn());
        commandParts.add("--agree-tos");
        commandParts.add("--non-interactive");
        commandParts.add("--email");
        commandParts.add(email);

        return String.join(" ", commandParts);
    }

    private String buildCertbotRenewalCron() {
        return "SLEEPTIME=$(awk 'BEGIN{srand(); print int(rand()*(3600+1))}'); echo \"0 0,12 * * * root sleep $SLEEPTIME && certbot renew -q\" | sudo tee -a /etc/crontab > /dev/null";
    }

    private List<String> buildEstRequiredInputs(ServerEntry serverEntry) throws Exception {
        Optional<AuthCredential> optionalAuthCredential = getAuthCredential(serverEntry);

        List<String> parts = new LinkedList<>();

        if (optionalAuthCredential.isPresent()) {
            AuthCredential authCredential = optionalAuthCredential.get();
            parts.add("Enrollment URL: " + estBaseUrl);
            parts.add("HTTP Client Username: " + authCredential.getKeyIdentifier());
            parts.add("HTTP Client Password: " + authCredential.getMacKeyBase64());

        }
        return parts;
    }

    private String buildWinAcmeRequiredInputs(ServerEntry serverEntry, String applicationName) throws Exception {
        Optional<AuthCredential> optionalAuthCredential = getAuthCredential(serverEntry);

        if (optionalAuthCredential.isPresent()) {
            AuthCredential credential = optionalAuthCredential.get();
            List<String> commandParts = new LinkedList<>();
            commandParts.add("./wacs.exe");
            commandParts.add("--source");
            commandParts.add("--accepttos");
            commandParts.add(applicationName);
            if(applicationName.equalsIgnoreCase("iis")){
                commandParts.add("--installation iis");
            }
            commandParts.add("--siteid s");
            commandParts.add("--host");
            commandParts.add(serverEntry.getFqdn());
            commandParts.add("--baseuri");
            commandParts.add(acmeBaseUrl + "/acme/directory");
            commandParts.add("--eab-key-identifier");
            commandParts.add(credential.getKeyIdentifier());
            commandParts.add("--eab-key");
            commandParts.add(credential.getMacKeyBase64());
            commandParts.add("--force");

            return String.join(" ", commandParts);
        } else {
            throw new RAObjectNotFoundException(AuthCredential.class, serverEntry.getFqdn());
        }
    }

    private Optional<AuthCredential> getAuthCredential(ServerEntry serverEntry) throws Exception {
        UniqueEntityLookupForm lookupForm = new UniqueEntityLookupForm();
        lookupForm.setObjectClass(ServerEntry.class.getCanonicalName());
        lookupForm.setObjectUuid(serverEntry.getUuid());

        List<AuthCredential> validAuthCredentials = authCredentialService.getValidAuthCredentials(lookupForm);
        if (CollectionUtils.isNotEmpty(validAuthCredentials)) {
            AuthCredential credential = validAuthCredentials.get(0);
            return Optional.of(credential);
        } else {
            return Optional.empty();
        }
    }

    private static class CertManagerClusterIssuer {
        private CertManagerClusterIssuerMetadata metadata;
        private CertManagerClusterIssuerSpec spec;
    }

    private static class CertManagerClusterIssuerMetadata {
        private String name;

    }

    private static class CertManagerClusterIssuerSpec {
        private CertManagerClusterIssuerSpecAcme acme;
    }

    private static class CertManagerClusterIssuerSpecAcme {
        private String skipTLSVerify;
        private String email;
        private String server;
        private CertManagerClusterIssuerSpecAcmeEab externalAccountBinding;

    }

    private static class CertManagerClusterIssuerSpecAcmeEab {
        private String keyID;
        private CertManagerClusterIssuerSpecAcmeEabKeySecretRef keySecretRef;
        private String keyAlgorithm;

    }

    private static class CertManagerClusterIssuerSpecAcmeEabKeySecretRef {
        private String name;
        private String key;
    }
}
