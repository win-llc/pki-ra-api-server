package com.winllc.pki.ra.service.transaction;

import com.winllc.acme.common.SubjectAltNames;
import com.winllc.acme.common.cache.CachedCertificateService;
import com.winllc.acme.common.domain.Account;
import com.winllc.acme.common.ra.RACertificateIssueRequest;
import com.winllc.pki.ra.beans.form.ServerEntryForm;
import com.winllc.acme.common.ca.CertAuthority;
import com.winllc.acme.common.constants.AuditRecordType;
import com.winllc.acme.common.domain.*;
import com.winllc.pki.ra.exception.RAException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.AuditRecordRepository;
import com.winllc.acme.common.repository.CertificateRequestRepository;
import com.winllc.acme.common.repository.ServerEntryRepository;
import com.winllc.pki.ra.service.CertAuthorityConnectionService;
import com.winllc.pki.ra.service.ServerEntryService;
import com.winllc.pki.ra.service.ServerSettingsService;
import com.winllc.pki.ra.service.external.EntityDirectoryService;
import com.winllc.pki.ra.util.FormValidationUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.context.ApplicationContext;

import javax.transaction.Transactional;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.winllc.pki.ra.constants.ServerSettingRequired.ENTITY_DIRECTORY_LDAP_SERVERBASEDN;

public class CertIssuanceTransaction extends CertTransaction {

    private static final Logger log = LogManager.getLogger(CertAuthorityConnectionService.class);

    private final ServerSettingsService serverSettingsService;
    private final ServerEntryService serverEntryService;
    private final ServerEntryRepository serverEntryRepository;
    private final CertificateRequestRepository certificateRequestRepository;
    private final EntityDirectoryService entityDirectoryService;
    private final AuditRecordRepository auditRecordRepository;
    private final CachedCertificateService cachedCertificateService;

    public CertIssuanceTransaction(CertAuthority certAuthority, ApplicationContext context) {
        super(certAuthority, context);
        this.serverSettingsService = context.getBean(ServerSettingsService.class);
        this.serverEntryService = context.getBean(ServerEntryService.class);
        this.serverEntryRepository = context.getBean(ServerEntryRepository.class);
        this.certificateRequestRepository = context.getBean(CertificateRequestRepository.class);
        this.entityDirectoryService = context.getBean(EntityDirectoryService.class);
        this.auditRecordRepository = context.getBean(AuditRecordRepository.class);
        this.cachedCertificateService = context.getBean(CachedCertificateService.class);
    }

    /**
     * Process a certificate request sent to the RA
     * @param certificateRequest
     * @return
     * @throws Exception
     */
    @Transactional
    public X509Certificate processIssueCertificate(RACertificateIssueRequest certificateRequest) throws Exception {
        return processIssueCertificate(certificateRequest, null);
    }

    @Transactional
    public X509Certificate processIssueCertificate(RACertificateIssueRequest certificateRequest, Account account) throws Exception {
        //todo validation checks, probably before this, notify POCs on failure
        SubjectAltNames subjectAltNames = new SubjectAltNames();
        for (String dnsName : certificateRequest.getDnsNameList()) {
            if(FormValidationUtil.isValidFqdn(dnsName)) {
                subjectAltNames.addValue(SubjectAltNames.SubjAltNameType.DNS, dnsName);
            }else{
                log.error("Will not add invalid fqdn: "+dnsName);
            }
        }

        String buildDn = buildDn(certificateRequest);

        SystemActionRunner runner = SystemActionRunner.build(this.context);

        ThrowingSupplier<X509Certificate, Exception> action = () -> {
            X509Certificate cert = certAuthority.issueCertificate(certificateRequest.getCsr(), buildDn, subjectAltNames);
            if (cert != null) {
                if(account != null) {
                    processIssuedCertificate(cert, certificateRequest, account);
                }else{
                    log.info("Anonymous cert issued: "+cert.getSubjectDN());
                }
                return cert;
            } else {
                throw new RAException("Could not issue certificate");
            }
        };

        X509Certificate cert = runner.execute(action);

        ThrowingSupplier<X509Certificate, Exception> postProcessAction = () -> {
            if (cert != null) {
                cachedCertificateService.persist(cert, certificateRequest.getCertAuthorityName());
                return cert;
            } else {
                throw new RAException("Could not cache certificate");
            }
        };

        SystemActionRunner postProcessRunner = SystemActionRunner.build(this.context);
        postProcessRunner.executeAsync(postProcessAction);

        return cert;
    }

    private String buildDn(RACertificateIssueRequest certificateRequest){
        String dn = "CN=";

        if(StringUtils.isNotBlank(certificateRequest.getSubjectNameRequest())){
            dn += certificateRequest.getSubjectNameRequest();
        }else{
            //select first fqdn
            dn += certificateRequest.getDnsNameList().get(0);
        }

        Optional<String> serverBaseDnOptional = serverSettingsService.getServerSettingValue(ENTITY_DIRECTORY_LDAP_SERVERBASEDN);

        if(serverBaseDnOptional.isPresent()) {
            dn += "," + serverBaseDnOptional.get();
        }
        return dn;
    }

    /**
     * Post certificate issuance actions
     * @param certificate
     * @param raCertificateIssueRequest
     * @param account
     * @throws CertificateEncodingException
     * @throws RAObjectNotFoundException
     */
    private void processIssuedCertificate(X509Certificate certificate, RACertificateIssueRequest raCertificateIssueRequest,
                                          Account account)
            throws CertificateEncodingException, RAException {

        ServerEntry serverEntry;
        String subjectNoSpaces = certificate.getSubjectDN().getName().replace(", ", ",");
        Optional<ServerEntry> optionalServerEntry =
                serverEntryRepository.findDistinctByDistinguishedNameIgnoreCaseAndAccount(subjectNoSpaces, account);

        //If server entry does not exist, create one
        if(optionalServerEntry.isPresent()){
            serverEntry = optionalServerEntry.get();
        }else{
            String fqdn;
            if(StringUtils.isNotBlank(raCertificateIssueRequest.getSubjectNameRequest())){
                fqdn = raCertificateIssueRequest.getSubjectNameRequest();
            }else if(raCertificateIssueRequest.getDnsNameList().size() > 0){
                fqdn = raCertificateIssueRequest.getDnsNameList().get(0);
            }else{
                throw new RAException("No Subject Name available");
            }

            ServerEntryForm form = new ServerEntryForm();
            form.setAccountId(account.getId());
            form.setFqdn(fqdn);
            form.setAlternateDnsValues(raCertificateIssueRequest.getDnsNameList());
            Long serverEntryId = serverEntryService.createServerEntry(form);
            serverEntry = serverEntryRepository.findById(serverEntryId).get();
        }

        //If certificate request does not exist, create one
        CertificateRequest certificateRequest;
        if(raCertificateIssueRequest.getExistingCertificateRequestId() == null) {
            certificateRequest = new CertificateRequest();
            certificateRequest.setAccount(account);
            certificateRequest.setCsr(raCertificateIssueRequest.getCsr());
            certificateRequest = certificateRequestRepository.save(certificateRequest);
        }else{
            Optional<CertificateRequest> optionalRequest
                    = certificateRequestRepository.findById(raCertificateIssueRequest.getExistingCertificateRequestId());
            if(optionalRequest.isPresent()){
                certificateRequest = optionalRequest.get();
                Hibernate.initialize(certificateRequest.getRequestedDnsNames());
            }else{
                log.error("Expected an existing certificate request, but found none");
                throw new RAObjectNotFoundException(CertificateRequest.class, raCertificateIssueRequest.getExistingCertificateRequestId());
            }
        }

        certificateRequest.setCertAuthorityName(raCertificateIssueRequest.getCertAuthorityName());
        certificateRequest.setStatus("issued");
        certificateRequest.setSubmittedOn(Timestamp.valueOf(LocalDateTime.now()));
        certificateRequest.addIssuedCertificate(certificate);
        certificateRequest.setServerEntry(serverEntry);
        certificateRequest = certificateRequestRepository.save(certificateRequest);

        Hibernate.initialize(serverEntry.getCertificateRequests());

        serverEntry.setDistinguishedName(certificate.getSubjectDN().getName());
        serverEntry.getCertificateRequests().add(certificateRequest);
        serverEntry = serverEntryRepository.save(serverEntry);

        try {
            entityDirectoryService.applyServerEntryToDirectory(serverEntry);
        }catch (Exception e){
            log.error("Could not process", e);
        }

        AuditRecord record = AuditRecord.buildNew(AuditRecordType.CERTIFICATE_ISSUED, serverEntry)
                .addAccountKid(raCertificateIssueRequest.getAccountKid())
                .addSource(raCertificateIssueRequest.getSource());
        auditRecordRepository.save(record);
    }
}
