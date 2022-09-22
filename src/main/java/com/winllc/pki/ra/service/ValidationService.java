package com.winllc.pki.ra.service;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.Base64URL;
import com.winllc.acme.common.CertIssuanceValidationResponse;
import com.winllc.acme.common.CertIssuanceValidationRule;
import com.winllc.acme.common.domain.Account;
import com.winllc.acme.common.ra.RAAccountValidationResponse;
import com.winllc.pki.ra.beans.form.CertificateValidationForm;
import com.winllc.acme.common.domain.*;
import com.winllc.pki.ra.exception.RAException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.*;
import com.winllc.ra.integration.ca.CertAuthority;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.naming.InvalidNameException;
import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/validation")
public class ValidationService {

    private static final Logger log = LogManager.getLogger(ValidationService.class);

    private final AccountRepository accountRepository;
    private final ServerEntryRepository serverEntryRepository;
    private final AccountRestrictionService accountRestrictionService;
    private final AuthCredentialRepository authCredentialRepository;
    @Autowired
    private CertificateRequestRepository certificateRequestRepository;
    @Autowired
    private CertAuthorityConnectionService certAuthorityConnectionService;
    @Autowired
    private AuthCredentialService authCredentialService;

    public ValidationService(AccountRepository accountRepository, ServerEntryRepository serverEntryRepository,
                             AccountRestrictionService accountRestrictionService, AuthCredentialRepository authCredentialRepository) {
        this.accountRepository = accountRepository;
        this.serverEntryRepository = serverEntryRepository;
        this.accountRestrictionService = accountRestrictionService;
        this.authCredentialRepository = authCredentialRepository;
    }

    @PostMapping("/rules/{kid}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public CertIssuanceValidationResponse getAccountValidationRules(@PathVariable String kid) throws RAException {

        Optional<AuthCredential> optionalAuthCredential = authCredentialRepository.findDistinctByKeyIdentifier(kid);
        if(optionalAuthCredential.isPresent()){
            AuthCredential authCredential = optionalAuthCredential.get();
            Hibernate.initialize(authCredential.getParentEntity());
            AuthCredentialHolder parentEntity = authCredential.getParentEntity();

            CertIssuanceValidationResponse response = new CertIssuanceValidationResponse(authCredential.getKeyIdentifier());
            response.setCertIssuanceValidationRules(getRulesForEntity(parentEntity));

            //todo re-add account check
            //boolean accountValid = accountRestrictionService.checkIfAccountValid(account);
            //response.setAccountIsValid(accountValid);

            response.setAccountIsValid(true);

            return response;
        } else {
            throw new RAObjectNotFoundException(Account.class, kid);
        }
    }

    private List<CertIssuanceValidationRule> getRulesForEntity(AuthCredentialHolder holder) throws RAException {
        List<CertIssuanceValidationRule> rules = new ArrayList<>();

        if(holder instanceof Account){
            Account account = (Account) holder;
            Set<DomainPolicy> accountDomainPolicies = account.getAccountDomainPolicies();

            Hibernate.initialize(account.getAccountDomainPolicies());

            for (DomainPolicy domainPolicy : accountDomainPolicies) {
                rules.add(buildRuleForDomainPolicy(domainPolicy));
            }

        }else if(holder instanceof DomainPolicy){
            DomainPolicy domainPolicy = (DomainPolicy) holder;
            rules.add(buildRuleForDomainPolicy(domainPolicy));

        }else if(holder instanceof ServerEntry){
            ServerEntry serverEntry = (ServerEntry) holder;

            CertIssuanceValidationRule validationRule = new CertIssuanceValidationRule();
            validationRule.setAllowIssuance(true);
            validationRule.setIdentifierType("dns");
            validationRule.setAllowHostnameIssuance(true);
            validationRule.setBaseDomainName(serverEntry.getFqdn());

            //todo make dynamic
            validationRule.setRequireHttpChallenge(false);
            validationRule.setRequireDnsChallenge(false);

            rules.add(validationRule);

        }else{
            throw new RAException("Could not find a valid object for AuthCredentialHolder");
        }

        return rules;
    }

    private CertIssuanceValidationRule buildRuleForDomainPolicy(DomainPolicy domainPolicy){
        Domain domain = domainPolicy.getTargetDomain();
        Account account = domainPolicy.getAccount();

        Map<String, DomainPolicy> restrictionMap = account.getAccountDomainPolicies().stream()
                .collect(Collectors.toMap(r -> r.getTargetDomain().getFullDomainName(), r -> r));

        CertIssuanceValidationRule validationRule = new CertIssuanceValidationRule();
        validationRule.setBaseDomainName(domain.getFullDomainName());
        validationRule.setAllowHostnameIssuance(account.isAllowHostnameIssuance());
        validationRule.setIdentifierType("dns");

        //If restrictions exist for this domain on the account, apply restrictions
        if(restrictionMap.containsKey(domain.getFullDomainName())){
            DomainPolicy restriction = restrictionMap.get(domain.getFullDomainName());
            validationRule.setRequireHttpChallenge(restriction.isAcmeRequireHttpValidation());
            validationRule.setRequireDnsChallenge(restriction.isAcmeRequireDnsValidation());
            validationRule.setAllowIssuance(restriction.isAllowIssuance());
        }else{
            validationRule.setAllowIssuance(true);
            validationRule.setRequireHttpChallenge(false);
        }

        return validationRule;
    }

    @GetMapping("/account/preAuthzIdentifiers/{kid}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public Set<String> getAccountPreAuthorizedIdentifiers(@PathVariable String kid) throws RAObjectNotFoundException {
        Optional<Account> optionalAccount = authCredentialService.getAssociatedAccount(kid);

        if (optionalAccount.isPresent()) {
            Account account = optionalAccount.get();

            List<ServerEntry> allByAccount = serverEntryRepository.findAllByAccount(account);
            Set<String> preAuthzIdentifiers = allByAccount.stream()
                    .filter(s -> s.getAcmeAllowPreAuthz())
                    .map(s -> s.getFqdn())
                    .collect(Collectors.toSet());

            return preAuthzIdentifiers;
        } else {
            throw new RAObjectNotFoundException(Account.class, kid);
        }
    }

    @PostMapping("/account/verify")
    @ResponseStatus(HttpStatus.OK)
    public Boolean verifyExternalAccountBinding(@RequestParam String macKey, @RequestParam String keyIdentifier,
                                                          @RequestParam String jwsObject, @RequestParam String accountObject)
            throws RAException {
        Base64URL macKeyBase64 = new Base64URL(macKey);

        log.info("MAC Key: " + macKeyBase64.toString());
        log.info("Key Identifier: " + keyIdentifier);

        Optional<AuthCredential> optionalCredential = authCredentialRepository.findDistinctByKeyIdentifier(keyIdentifier);

        if (optionalCredential.isPresent()) {
            AuthCredential authCredential = optionalCredential.get();
            //todo check expiration
            if(!authCredential.getValid()){
                log.info("Credential presented is not valid");
                return false;
            }

            try {
                JWSObject jwsObjectParsed = JWSObject.parse(jwsObject);
                JWSObject accountJWSParsed = JWSObject.parse(accountObject);
                JWSSigner signer = new MACSigner(authCredential.getMacKey());

                JWSObject testObj = new JWSObject(jwsObjectParsed.getHeader(), jwsObjectParsed.getPayload());
                testObj.sign(signer);

                log.info("Test signed obj: " + testObj.getSignature().toJSONString());

                if (testObj.getSignature().toString().contentEquals(jwsObjectParsed.getSignature().toString())) {
                    log.info("Account request verified!");
                    return true;
                } else {
                    throw new RAException("Could not verify EAB, signatures did not match");
                }
            } catch (Exception e) {
                log.error("Invalid key length", e);
                throw new RAException("Could not verify External Account Binding for KID: " + keyIdentifier);
            }

        } else {
            throw new RAObjectNotFoundException(Account.class, keyIdentifier);
        }
    }

    @GetMapping("/account/getCanIssueDomains/{kid}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<String> getCanIssueDomains(@PathVariable String kid) throws Exception {

        Optional<Account> optionalAccount = authCredentialService.getAssociatedAccount(kid);
        if(optionalAccount.isPresent()){
            Account account = optionalAccount.get();
            Set<DomainPolicy> accountDomainPolicies = account.getAccountDomainPolicies();
            List<String> domainList = accountDomainPolicies
                    .stream()
                    .map(DomainPolicy::getTargetDomain)
                    .map(Domain::getFullDomainName)
                    .collect(Collectors.toList());

            return domainList;
        }else{
            throw new RAObjectNotFoundException(Account.class, kid);
        }
    }

    public boolean canIssueToServer(String kid, String serverFqdn){
        //todo
        Optional<AuthCredential> optionalCredential = authCredentialRepository.findDistinctByKeyIdentifier(kid);
        if(optionalCredential.isPresent()){

        }
        return false;
    }

    @PostMapping("/account/validateCredentials")
    public RAAccountValidationResponse validateAccountCredentials(@RequestParam("accountId") String accountId,
                                                                  @RequestParam("password") String password) {
        RAAccountValidationResponse response = new RAAccountValidationResponse();
        response.setValid(false);

        Optional<AuthCredential> optionalCredential = authCredentialRepository.findDistinctByKeyIdentifier(accountId);

        if(optionalCredential.isPresent()){
            AuthCredential authCredential = optionalCredential.get();

            Base64 accountMacKeyBase64 = new Base64(authCredential.getMacKeyBase64());
            byte[] decodedMacKey = accountMacKeyBase64.decode();

            Base64 passwordBase64 = new Base64(password);
            byte[] decodedPassword = passwordBase64.decode();

            if(Arrays.equals(decodedPassword, decodedMacKey)){
                response.setValid(true);
            }else{
                response.setMessage("AccountId and password combo failed");
            }
        }else{
            log.error("Could not find account: "+accountId);
            response.setMessage("No account found");
        }
        return response;
    }

    @Transactional
    @PostMapping("/account/validateServer")
    public RAAccountValidationResponse validateServerReEnrollment(@RequestBody CertificateValidationForm form)
            throws RAObjectNotFoundException, InvalidNameException {

        Optional<CertAuthority> optionalCa = certAuthorityConnectionService.getCertAuthorityByIssuerDn(form.getIssuerDn());
        if(optionalCa.isPresent()) {
            Optional<CertificateRequest> optionalCertificate
                    = certificateRequestRepository.findDistinctByIssuedCertificateSerialAndCertAuthorityName(
                            form.getSerial(), optionalCa.get().getName());

            if (optionalCertificate.isPresent()) {
                CertificateRequest certificate = optionalCertificate.get();
                Account account = certificate.getAccount();

                Optional<AuthCredential> latestCredential = authCredentialService.getLatestAuthCredentialForAccount(account);

                RAAccountValidationResponse response = new RAAccountValidationResponse();
                response.setAccountId(latestCredential.get().getKeyIdentifier());
                response.setValid(true);

                return response;
            } else {
                throw new RAObjectNotFoundException(CertificateRequest.class, form.getSerial() + " " + form.getIssuerDn());
            }
        }else{
            throw new RAObjectNotFoundException(CertAuthority.class, form.getIssuerDn());
        }
    }

}
