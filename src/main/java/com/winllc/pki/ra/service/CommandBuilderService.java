package com.winllc.pki.ra.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.winllc.pki.ra.beans.form.AcmeExternalAccountProviderSettingsForm;
import com.winllc.pki.ra.beans.form.UniqueEntityLookupForm;
import com.winllc.acme.common.domain.AuthCredential;
import com.winllc.acme.common.domain.ServerEntry;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
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
    public List<String> buildCertbotCommand(@PathVariable String command, @PathVariable Long id,
                                            @PathVariable String applicationName,
                                            Authentication authentication)
            throws Exception {
        ServerEntry serverEntry = serverEntryService.getServerEntry(id);

        List<String> commands = new LinkedList<>();

        switch (command) {
            case "certbot" -> {
                String registerCommand = buildCertbotRegisterCommand(serverEntry, authentication.getName());
                String certCommand = buildCertbotCertCommand(serverEntry, applicationName, authentication.getName());
                commands.add(registerCommand);
                commands.add(certCommand);
                commands.add(buildCertbotRenewalCron());
            }
            case "win-acme" -> commands.add(buildWinAcmeRequiredInputs(serverEntry, applicationName));
            case "est" -> commands.addAll(buildEstRequiredInputs(serverEntry));
            case "k8s" -> {
                AuthCredential authCredential = getAuthCredential(serverEntry).orElseThrow();
                commands.add(buildCertManagerEabSecret(authCredential));
                commands.add(buildCertManagerYamlConfig(authCredential));
            }
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
            commandParts.add(applicationName);
            if(applicationName.equalsIgnoreCase("iis")){
                commandParts.add("--installation iis");
            }
            commandParts.add("--accepttos");
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

    private String buildCertManagerEabSecret(AuthCredential authCredential){
        return "kubectl create secret generic eab-secret --namespace cert-manager --from-literal secret="+authCredential.getMacKeyBase64();
    }

    private String buildCertManagerYamlConfig(AuthCredential authCredential) throws JsonProcessingException {
        CertManagerClusterIssuerSpecAcme acme = new CertManagerClusterIssuerSpecAcme();
        acme.setServer(acmeBaseUrl+"/acme/directory");
        acme.setEmail("teste@test.com");
        acme.setSkipTLSVerify(true);

        CertManagerClusterIssuerMetadata metadata = new CertManagerClusterIssuerMetadata();
        metadata.setName("winllc-acme-server");

        CertManagerClusterIssuerSpecAcmeEabKeySecretRef secretRef = new CertManagerClusterIssuerSpecAcmeEabKeySecretRef();
        secretRef.setName("eab-secret");
        secretRef.setKey("secret");

        CertManagerClusterIssuerSpecAcmeEab eab = new CertManagerClusterIssuerSpecAcmeEab();
        eab.setKeyID(authCredential.getKeyIdentifier());
        eab.setKeySecretRef(secretRef);
        acme.setExternalAccountBinding(eab);

        CertManagerClusterIssuerSpec spec = new CertManagerClusterIssuerSpec();
        spec.setAcme(acme);

        CertManagerClusterIssuer issuer = new CertManagerClusterIssuer();
        issuer.setApiVersion("cert-manager.io/v1");
        issuer.setKind("ClusterIssuer");
        issuer.setMetadata(metadata);
        issuer.setSpec(spec);

        CertManagerClusterIssuerSolverIngress ingress = new CertManagerClusterIssuerSolverIngress();
        ingress.setIngressClass("public");

        CertManagerClusterIssuerSolver httpSolver = new CertManagerClusterIssuerSolver();
        httpSolver.setIngress(ingress);

        CertManagerClusterIssuerSolvers solvers = new CertManagerClusterIssuerSolvers();
        solvers.setHttp01(httpSolver);

        acme.getSolvers().add(solvers);

        CertManagerPrivateKeySecretRef keySecretRef = new CertManagerPrivateKeySecretRef();
        keySecretRef.setName("winra-account-key");
        acme.setPrivateKeySecretRef(keySecretRef);

        ObjectMapper om = new ObjectMapper(new YAMLFactory());
        return om.writeValueAsString(issuer);
    }

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class CertManagerClusterIssuer {
        private String apiVersion;
        private String kind;
        private CertManagerClusterIssuerMetadata metadata;
        private CertManagerClusterIssuerSpec spec;
    }

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class CertManagerClusterIssuerMetadata {
        private String name;

    }

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class CertManagerClusterIssuerSpec {
        private CertManagerClusterIssuerSpecAcme acme;

    }

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class CertManagerClusterIssuerSpecAcme {
        private boolean skipTLSVerify = false;
        private String email;
        private String server;
        private CertManagerClusterIssuerSpecAcmeEab externalAccountBinding;
        private CertManagerPrivateKeySecretRef privateKeySecretRef;
        private List<CertManagerClusterIssuerSolvers> solvers = new ArrayList<>();

    }

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class CertManagerPrivateKeySecretRef {
        private String name;
    }


    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class CertManagerClusterIssuerSpecAcmeEab {
        private String keyID;
        private CertManagerClusterIssuerSpecAcmeEabKeySecretRef keySecretRef;
        private String keyAlgorithm;
    }

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class CertManagerClusterIssuerSpecAcmeEabKeySecretRef {
        private String name;
        private String key;
    }

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class CertManagerClusterIssuerSolvers {

        private CertManagerClusterIssuerSolver http01;

    }

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class CertManagerClusterIssuerSolver {

        private CertManagerClusterIssuerSolverIngress ingress;
    }

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class CertManagerClusterIssuerSolverIngress {
        @JsonProperty("class")
        private String ingressClass;
    }
}
