package com.winllc.pki.ra.ca;

import com.netscape.certsrv.ca.CACertClient;
import com.netscape.certsrv.cert.*;
import com.netscape.certsrv.client.ClientConfig;
import com.netscape.certsrv.client.PKIClient;
import com.netscape.certsrv.client.SubsystemClient;
import com.netscape.certsrv.profile.ProfileAttribute;
import com.netscape.certsrv.profile.ProfileInput;
import com.netscape.certsrv.property.Descriptor;
import com.netscape.certsrv.request.RequestId;
import com.winllc.acme.common.CertSearchConverter;
import com.winllc.acme.common.CertSearchParam;
import com.winllc.acme.common.CertificateDetails;
import com.winllc.acme.common.SubjectAltNames;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.acme.common.util.HttpCommandUtil;
import com.winllc.pki.ra.domain.CertAuthorityConnectionInfo;
import com.winllc.pki.ra.keystore.ApplicationKeystore;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.mozilla.jss.netscape.security.x509.RevocationReason;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

public class DogTagCertAuthority extends AbstractCertAuthority {

    private static final Logger log = LogManager.getLogger(DogTagCertAuthority.class);

    private ApplicationKeystore applicationKeystore;

    private static final List<String> requiredProperties;
    private static final Map<String, String> defaultProperties;

    private String baseUrl = "https://dogtag.winllc.com:8443";

    static{
        requiredProperties = new ArrayList<>();
        requiredProperties.add("BASE_URL");

        defaultProperties = new HashMap<>();
    }

    public DogTagCertAuthority(CertAuthorityConnectionInfo info, ApplicationKeystore applicationKeystore) throws Exception {
        super(info);
        this.applicationKeystore = applicationKeystore;
        try {
            this.baseUrl = info.getPropertyByName("BASE_URL").get().getValue();
        }catch (Exception e){
            throw e;
        }
    }

    @Override
    public List<String> getRequiredConnectionProperties() {
        return requiredProperties;
    }

    @Override
    public Map<String, String> getDefaultProperties() {
        return defaultProperties;
    }

    @Override
    public X509Certificate issueCertificate(String csr, SubjectAltNames sans) throws Exception {
        String requestPath = "/ca/rest/certrequests";

        CertEnrollmentRequest var1 = new CertEnrollmentRequest();
        var1.setProfileId("caServerCert");
        var1.setRenewal(false);
        ProfileInput var2 = var1.createInput("CertReqInput");
        var2.addAttribute(new ProfileAttribute("cert_request_type", "pkcs10", (Descriptor)null));
        var2.addAttribute(new ProfileAttribute("cert_request", csr, (Descriptor)null));
        ProfileInput var4 = var1.createInput("SubmitterInfoInput");
        var4.addAttribute(new ProfileAttribute("requestor_name", "admin", (Descriptor)null));
        var4.addAttribute(new ProfileAttribute("requestor_email", "admin@redhat.com", (Descriptor)null));
        var4.addAttribute(new ProfileAttribute("requestor_phone", "650-555-5555", (Descriptor)null));
        var1.setAttribute("uid", "caadmin");
        var1.setAttribute("pwd", "P@ssW0rd");

        Function<String, CertRequestInfo> returnFunction = (s) -> {
            try {
                CertRequestInfos certRequestInfos = CertRequestInfos.valueOf(s);
                CertRequestInfo requestInfo = new ArrayList<>(certRequestInfos.getEntries()).get(0);
                log.info(certRequestInfos);
                return requestInfo;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        };

        //Submit the request
        CertRequestInfo val = processDogtagPostOperation(baseUrl+requestPath, var1, returnFunction, 200);

        if(val != null){
            if(val.getOperationResult().equalsIgnoreCase("success")){

                String requestId = val.getRequestURL().substring(val.getRequestURL().lastIndexOf("/") + 1);

                Function<String, CertReviewResponse> reviewFunction = (s) -> {
                    try {
                        return CertReviewResponse.fromXML(s);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                };

                //Retrieve the request
                CertReviewResponse certReviewResponse = processDogtagGetOperation(baseUrl+"/ca/rest/agent/certrequests/"+requestId, reviewFunction);

                Function<String, String> approveFunction = (s) -> s;

                //Approve the request
                processDogtagPostOperation(baseUrl+"/ca/rest/agent/certrequests/"+requestId+"/approve", certReviewResponse, approveFunction, 204);

                Function<String, CertRequestInfo> getRequestFunction = s -> {
                    try {
                        return CertRequestInfo.valueOf(s);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                };

                //Get the approved request
                CertRequestInfo requestInfo = processDogtagGetOperation(baseUrl + "/ca/rest/certrequests/"+requestId,getRequestFunction);

                return getCertificateBySerial(requestInfo.getCertId().toHexString());
            }
        }

        return null;
    }


    @Override
    public boolean revokeCertificate(String serial, int reason) throws Exception {
        String revokePath = "/ca/rest/agent/certs/";
        CertRevokeRequest request = new CertRevokeRequest();
        request.setReason(RevocationReason.fromInt(reason));

        X509Certificate certificateBySerial = getCertificateBySerial(serial);

        request.setEncoded(CertUtil.convertToPem(certificateBySerial));
        request.setInvalidityDate(Date.from(Instant.now()));

        Function<String, CertRequestInfo> process = s -> {
            try {
                return CertRequestInfo.valueOf(s);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        };

        CertRequestInfo requestInfo = processDogtagPostOperation(baseUrl + revokePath + serial + "/revoke", request, process, 200);

        if(requestInfo.getOperationResult().equalsIgnoreCase("success")){
            return true;
        }else {
            return false;
        }
    }

    @Override
    public String getCertificateStatus(String serial) {
        return null;
    }

    @Override
    public List<CertificateDetails> search(CertSearchParam params) {
        String searchPath = "/ca/rest/certs/search";

        CertSearchRequest certSearchRequest = new CertSearchRequest();


        //params.buildQuery()

        //todo
        return null;
    }

    @Override
    public Certificate[] getTrustChain() {
        //todo
        return new Certificate[0];
    }

    @Override
    public X509Certificate getCertificateBySerial(String serial) throws Exception {
        Function<String, CertData> getCertFunction = s -> {
            try {
                return CertData.valueOf(s);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        };

        CertData certData = processDogtagGetOperation(baseUrl + "/ca/rest/certs/"+serial, getCertFunction);

        return CertUtil.base64ToCert(certData.getEncoded());
    }


    private <T> T processDogtagGetOperation(String url, Function<String, T> processFunction) throws Exception {
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "application/xml");
        httpGet.setHeader("Content-type", "application/xml");
        return processDogtagOperation(httpGet, processFunction, 200);
    }

    private <T> T processDogtagPostOperation(String url, Object request, Function<String, T> processFunction, int expectedStatus) throws Exception {
        String postXml = "";
        if(request instanceof CertEnrollmentRequest){
            postXml = ((CertEnrollmentRequest) request).toXML();
        }else{
            postXml = request.toString();
        }

        HttpPost approvePost = new HttpPost(url);
        approvePost.setHeader("Accept", "application/xml");
        approvePost.setHeader("Content-type", "application/xml");
        approvePost.setEntity(new StringEntity(postXml));
        return processDogtagOperation(approvePost, processFunction, expectedStatus);
    }

    private <T> T processDogtagOperation(HttpRequestBase httpRequest, Function<String, T> processFunction, int expectedStatus) throws Exception {
        return HttpCommandUtil.processCustomWithClientAuth(httpRequest, expectedStatus, processFunction,
                applicationKeystore.getKeyStore(), applicationKeystore.getKeystorePassword());
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

}
