package com.winllc.pki.ra.ca;

import com.netscape.certsrv.cert.*;
import com.netscape.certsrv.profile.ProfileAttribute;
import com.netscape.certsrv.profile.ProfileInput;
import com.netscape.certsrv.profile.ProfilePolicySet;
import com.netscape.certsrv.property.Descriptor;
import com.winllc.acme.common.CertSearchConverter;
import com.winllc.acme.common.CertSearchParam;
import com.winllc.acme.common.CertificateDetails;
import com.winllc.acme.common.SubjectAltNames;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.acme.common.util.HttpCommandUtil;
import com.winllc.pki.ra.domain.CertAuthorityConnectionInfo;
import com.winllc.pki.ra.keystore.ApplicationKeystore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mozilla.jss.netscape.security.x509.RevocationReason;
import org.springframework.ldap.support.LdapNameBuilder;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DogTagCertAuthority extends AbstractCertAuthority {

    private static final Logger log = LogManager.getLogger(DogTagCertAuthority.class);

    private ApplicationKeystore applicationKeystore;

    //Constants
    private final String requestPath = "/ca/rest/certrequests";
    private final String revokePath = "/ca/rest/agent/certs/";
    private final String searchPath = "/ca/rest/certs/search";
    private final String retrieveCert = "/ca/rest/certs/{0}";

    private String baseUrl = "https://dogtag-ca.winllc-dev.com:8443";

    public DogTagCertAuthority(CertAuthorityConnectionInfo info, ApplicationKeystore applicationKeystore) throws Exception {
        super(info);
        this.applicationKeystore = applicationKeystore;
        try {
            //this.baseUrl = info.getPropertyByName("BASE_URL").get().getValue();
            //this.baseUrl = info.getBaseUrl();
        }catch (Exception e){
            throw e;
        }
    }

    @Override
    public X509Certificate issueCertificate(String csr, String dn, SubjectAltNames sans) throws Exception {
        CertEnrollmentRequest certEnrollmentRequest = buildCertEnrollmentRequest(csr);

        Function<String, CertRequestInfo> returnFunction = (s) -> {
            try {
                CertRequestInfos certRequestInfos = CertRequestInfos.valueOf(s);
                CertRequestInfo requestInfo = new ArrayList<>(certRequestInfos.getEntries()).get(0);
                log.info(certRequestInfos);
                return requestInfo;
            } catch (Exception e) {
                log.error("Could not build CertRequestInfos", e);
            }
            return null;
        };

        //Submit the request
        CertRequestInfo val = processDogtagPostOperation(baseUrl+requestPath, certEnrollmentRequest,
                returnFunction, 200, null);

        if(val != null){
            if(val.getOperationResult().equalsIgnoreCase("success")){

                String requestId = val.getRequestURL().substring(val.getRequestURL().lastIndexOf("/") + 1);

                Function<String, CertReviewResponse> reviewFunction = (s) -> {
                    try {
                        return CertReviewResponse.fromXML(s);
                    } catch (Exception e) {
                        log.error("Could not convert CertReviewResponse", e);
                    }
                    return null;
                };

                //Retrieve the request
                CertReviewResponse certReviewResponse = processDogtagGetOperation(baseUrl+"/ca/rest/agent/certrequests/"+requestId, reviewFunction);

                Function<String, String> approveFunction = (s) -> s;

                List<ProfilePolicySet> policySets = certReviewResponse.getPolicySets();
                policySets.stream().flatMap(ps -> ps.getPolicies().stream())
                        .filter(p -> p.getDef().getName().equalsIgnoreCase("Subject Name Default"))
                        .forEach(p -> {
                            p.getDef().getAttributes().stream()
                                    .filter(pa -> pa.getName().equalsIgnoreCase("name"))
                                    .forEach(pa -> pa.setValue(dn));
                        });


                //Approve the request
                processDogtagPostOperation(baseUrl+"/ca/rest/agent/certrequests/"+requestId+"/approve",
                        certReviewResponse, approveFunction, 204, null);

                Function<String, CertRequestInfo> getRequestFunction = s -> {
                    try {
                        return CertRequestInfo.valueOf(s);
                    } catch (Exception e) {
                        log.error("Could not convert CertRequestInfo", e);
                    }
                    return null;
                };

                //Get the approved request
                CertRequestInfo requestInfo = processDogtagGetOperation(baseUrl + "/ca/rest/certrequests/"+requestId,getRequestFunction);

                return getCertificateBySerial(requestInfo.getCertId().toHexString());
            }else{
                throw new Exception("Dogtag operation failed with result: "+val.getOperationResult());
            }
        }else{
            throw new Exception("Could not process Dogtag certificate request");
        }
    }


    @Override
    public boolean revokeCertificate(String serial, int reason) throws Exception {
        CertRevokeRequest request = new CertRevokeRequest();
        request.setReason(RevocationReason.fromInt(reason));

        X509Certificate certificateBySerial = getCertificateBySerial(serial);

        request.setEncoded(CertUtil.formatCrtFileContents(certificateBySerial));
        request.setInvalidityDate(Date.from(Instant.now()));
        request.setComments("Revoke certificate");

        Function<String, CertRequestInfo> process = s -> {
            try {
                return CertRequestInfo.valueOf(s);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        };

        CertRequestInfo requestInfo = processDogtagPostOperation(baseUrl + revokePath + serial + "/revoke",
                request, process, 200, null);

        if(requestInfo.getOperationResult().equalsIgnoreCase("success")){
            return true;
        }else {
            return false;
        }
    }

    @Override
    public String getCertificateStatus(String serial) throws Exception {
        CertData certData = getCertDataBySerial(serial);

        return certData.getStatus();
    }

    @Override
    public List<CertificateDetails> search(CertSearchParam param) {

        CertSearchRequest certSearchRequest = new CertSearchRequest();

        switch (param.getField()){
            case SERIAL:
                certSearchRequest.setSerialFrom(param.getValue());
                certSearchRequest.setSerialTo(param.getValue());
                break;
            case ISSUER:
                certSearchRequest.setIssuerDN(param.getValue());
                break;
            case STATUS:
                certSearchRequest.setStatus(param.getValue());
                break;
            case SUBJECT:
                LdapName ldapName = LdapNameBuilder
                        .newInstance(param.getValue())
                        .build();
                Rdn cnRdn = ldapName.getRdn(0);
                String cn = cnRdn.getValue().toString();
                certSearchRequest.setCommonName(cn);
                certSearchRequest.setSubjectInUse(true);
                break;
        }

        Function<String, CertDataInfos> process = s -> {
            try {
                XMLStreamReader streamReader = XMLInputFactory.newFactory().createXMLStreamReader(new StringReader(s));

                JAXBContext jaxbContext     = JAXBContext.newInstance( CertDataInfos.class );
                Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                return (CertDataInfos) jaxbUnmarshaller.unmarshal(streamReader);
            } catch (Exception e) {
                log.error("Could not unmarshal xml", e);
            }
            return null;
        };

        List<CertificateDetails> certificateDetailsList = new ArrayList<>();
        try {

            Map<String, String> urlParams = null;
            if(param.isPaginated()) {
                urlParams = new HashMap<>();
                Integer start = param.getPage()*param.getPageSize();
                urlParams.put("start", start.toString());
                urlParams.put("size", ""+param.getPageSize());
            }

            CertDataInfos certDataInfos = processDogtagPostOperation(baseUrl + searchPath, certSearchRequest,
                    process, 200, urlParams);

            if(certDataInfos.getEntries() != null){
                certificateDetailsList = certDataInfos.getEntries().stream()
                        .map(c -> convertCertDataInfo(c))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Could not parse search results", e);
        }

        return certificateDetailsList;
    }

    private CertificateDetails convertCertDataInfo(CertDataInfo certDataInfo){
        CertificateDetails details = new CertificateDetails();
        details.setSerial(certDataInfo.getID().toHexString());
        details.setIssuer(certDataInfo.getIssuerDN());
        details.setSubject(certDataInfo.getSubjectDN());
        details.setStatus(certDataInfo.getStatus());
        return details;
    }

    @Override
    public X509Certificate getCertificateBySerial(String serial) throws Exception {
        CertData certData = getCertDataBySerial(serial);

        return CertUtil.base64ToCert(certData.getEncoded());
    }

    private CertData getCertDataBySerial(String serial) throws Exception {
        Function<String, CertData> getCertFunction = s -> {
            try {
                return CertData.valueOf(s);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        };

        CertData certData = processDogtagGetOperation(baseUrl + MessageFormat.format(retrieveCert, serial), getCertFunction);
        return certData;
    }


    private <T> T processDogtagGetOperation(String url, Function<String, T> processFunction) throws Exception {
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "application/xml");
        httpGet.setHeader("Content-type", "application/xml");
        return processDogtagOperation(httpGet, processFunction, 200);
    }

    private <T> T processDogtagPostOperation(String url, Object request, Function<String, T> processFunction,
                                             int expectedStatus, Map<String, String> urlParams) throws Exception {
        String postXml = "";
        if(request instanceof CertEnrollmentRequest){
            postXml = ((CertEnrollmentRequest) request).toXML();
        }else{
            //Create JAXB Context
            JAXBContext jaxbContext = JAXBContext.newInstance(request.getClass());
            //Create Marshaller
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            //Required formatting??
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            //Print XML String to Console
            StringWriter sw = new StringWriter();
            //Write XML to StringWriter
            jaxbMarshaller.marshal(request, sw);
            //Verify XML Content
            postXml = sw.toString();
        }

        URIBuilder uriBuilder = new URIBuilder(url);
        if(urlParams != null){
            for(Map.Entry<String, String> paramEntry : urlParams.entrySet()){
                uriBuilder.setParameter(paramEntry.getKey(), paramEntry.getValue());
            }
        }

        HttpPost approvePost = new HttpPost(uriBuilder.build());
        approvePost.setHeader("Accept", "application/xml");
        approvePost.setHeader("Content-type", "application/xml");
        approvePost.setEntity(new StringEntity(postXml));

        return processDogtagOperation(approvePost, processFunction, expectedStatus);
    }

    private <T> T processDogtagOperation(HttpRequestBase httpRequest, Function<String, T> processFunction, int expectedStatus)
            throws Exception {
        return HttpCommandUtil.processCustomWithClientAuth(httpRequest, expectedStatus, processFunction,
                applicationKeystore.getKeyStore(), applicationKeystore.getKeystorePassword(),
                //todo replace with connection properties alias
                "dogtag-admin");
    }

    private class DogtagSearchConverter implements CertSearchConverter<CertSearchRequest> {
        //todo
        @Override
        public CertSearchRequest convert(CertSearchParam param) {

            CertSearchRequest searchRequest = new CertSearchRequest();

            switch (param.getField()){
                case SERIAL:
                    //searchRequest.setSerialTo();
                    break;
            }

            return null;
        }

        @Override
        public CertSearchRequest convertAnd(CertSearchParam param) {
            return null;
        }

        @Override
        public CertSearchRequest convertOr(CertSearchParam param) {
            return null;
        }
    }

    private CertEnrollmentRequest buildCertEnrollmentRequest(String csr){
        CertEnrollmentRequest request = new CertEnrollmentRequest();
        request.setProfileId("caServerCert");
        request.setRenewal(false);
        ProfileInput var2 = request.createInput("CertReqInput");
        var2.addAttribute(new ProfileAttribute("cert_request_type", "pkcs10", (Descriptor)null));
        var2.addAttribute(new ProfileAttribute("cert_request", csr, (Descriptor)null));
        ProfileInput var4 = request.createInput("SubmitterInfoInput");
        var4.addAttribute(new ProfileAttribute("requestor_name", "none", (Descriptor)null));
        var4.addAttribute(new ProfileAttribute("requestor_email", "none", (Descriptor)null));
        var4.addAttribute(new ProfileAttribute("requestor_phone", "none", (Descriptor)null));

        //todo replace with property
        request.setAttribute("uid", "caadmin");
        //todo replace with property
        request.setAttribute("pwd", "P@ssW0rd");
        return request;
    }

}
