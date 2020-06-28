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
import org.apache.http.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mozilla.jss.netscape.security.x509.RevocationReason;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

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
            this.baseUrl = info.getBaseUrl();
        }catch (Exception e){
            throw e;
        }
    }

    @Override
    public X509Certificate issueCertificate(String csr, SubjectAltNames sans) throws Exception {
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
        CertRequestInfo val = processDogtagPostOperation(baseUrl+requestPath, certEnrollmentRequest, returnFunction, 200);

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
                                    .forEach(pa -> pa.setValue("CN="+sans.getSans().get(SubjectAltNames.SubjAltNameType.DNS).get(0)));
                        });


                //Approve the request
                processDogtagPostOperation(baseUrl+"/ca/rest/agent/certrequests/"+requestId+"/approve", certReviewResponse, approveFunction, 204);

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
    public String getCertificateStatus(String serial) throws Exception {
        //todo
        CertData certData = getCertDataBySerial(serial);

        return certData.getStatus();
    }

    @Override
    public List<CertificateDetails> search(CertSearchParam params) {

        CertSearchRequest certSearchRequest = new CertSearchRequest();


        //params.buildQuery()

        //todo
        return null;
    }

    /*
    @Override
    public Certificate[] getTrustChain() throws Exception {
        //todo iterate

        //todo this should be pulled from the connection properties

        String trustChain = getInfo().getTrustChainBase64();

        X509Certificate rootCa = CertUtil.base64ToCert(trustChain);

        return new Certificate[]{rootCa};

        String rootCa = "-----BEGIN CERTIFICATE-----\n" +
                "MIIEBDCCAuygAwIBAgIBATANBgkqhkiG9w0BAQsFADBtMRMwEQYKCZImiZPyLGQB\n" +
                "GRYDY29tMRowGAYKCZImiZPyLGQBGRYKd2lubGxjLWRldjETMBEGCgmSJomT8ixk\n" +
                "ARkWA3BraTESMBAGCgmSJomT8ixkARkWAmNhMREwDwYDVQQDDAhXSU4gUk9PVDAe\n" +
                "Fw0yMDA0MDQxODE3NTRaFw00MDA0MDQxODE3NTRaMG0xEzARBgoJkiaJk/IsZAEZ\n" +
                "FgNjb20xGjAYBgoJkiaJk/IsZAEZFgp3aW5sbGMtZGV2MRMwEQYKCZImiZPyLGQB\n" +
                "GRYDcGtpMRIwEAYKCZImiZPyLGQBGRYCY2ExETAPBgNVBAMMCFdJTiBST09UMIIB\n" +
                "IjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAt+B+eCFB412ZLI+Rkl9vRLLR\n" +
                "M3l/5xSUNeqj8rV6GAIv2JUSjD2by3o/52pncPJnc1iOCzxe79GXb1bXD6QJdNCJ\n" +
                "nDaUq585owtpBNFO+wl5cdtblmJaJJapuiM5xeis9E60ENulM3EKDanjtudWN62r\n" +
                "bGJxJ9m/LILt3x0wPad3Vsw/RA7bi66kxodBunioM9mc/4tlRE/GcVyYupfWYGh4\n" +
                "GHffH+q5HVHVn/NNpYWPtbddXgoihzuIOG6rIm2J+8nywO3i2zMZU7EFfWtUXPwn\n" +
                "ZqcGbp6UdbujctCYbcGP17KZgw6mbPnseS5kwlnkpjlvmZGdTS2D+MN0PR4aQQID\n" +
                "AQABo4GuMIGrMB8GA1UdIwQYMBaAFEYSFL8wPwK+4wfccifXK1pyFHG7MA8GA1Ud\n" +
                "EwEB/wQFMAMBAf8wDgYDVR0PAQH/BAQDAgHGMB0GA1UdDgQWBBRGEhS/MD8CvuMH\n" +
                "3HIn1ytachRxuzBIBggrBgEFBQcBAQQ8MDowOAYIKwYBBQUHMAGGLGh0dHA6Ly9k\n" +
                "b2d0YWctY2Eud2lubGxjLWRldi5jb206ODA4MC9jYS9vY3NwMA0GCSqGSIb3DQEB\n" +
                "CwUAA4IBAQCHUoQsQV3c+0tg7fL5E51HDB/sNJFQ/JPd93PJAq5KSWIdx3GjjkNb\n" +
                "bg2xonZz8x9A0M4WBODkTOX5DHrfnEK4I91yAezppKytKKGdx8258wVN1MV/kMdb\n" +
                "vGpWI4TrA/yzjZVOrDnJiBFPxGoep4ESnOEVP72oY92903KcytDKMbeFbTHSqZFl\n" +
                "O8t3TnqemWAK+q4CiNcRNKpLGRT2YPDFyKK1gIv0WSMnHSL4Nn0vIQnFEZgd/MIe\n" +
                "1iqwYSpQUEyzUMUUSVtb3aAGmxuPKN4p2hIpB+5KdU08vCt1W8kga+6szPb7umUg\n" +
                "w4cVuU8Kktg9dX8yDu4nr5KIh7s/Iog9\n" +
                "-----END CERTIFICATE-----";

        try {
            X509Certificate cert = CertUtil.base64ToCert(rootCa);
            return Collections.singletonList(cert).toArray(new Certificate[0]);
        }catch (Exception e){
            e.printStackTrace();
        }
        return new Certificate[0];
    }

     */

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
