package com.winllc.pki.ra.mock;

import com.winllc.acme.common.CertSearchParam;
import com.winllc.acme.common.CertSearchParams;
import com.winllc.acme.common.CertificateDetails;
import com.winllc.acme.common.SubjectAltNames;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.ca.CertAuthority;
import com.winllc.pki.ra.ca.CertAuthorityConnectionType;

import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

public class MockCertAuthority implements CertAuthority {

    public static final String testX509Cert = "-----BEGIN CERTIFICATE-----\n" +
            "MIID1DCCArygAwIBAgIBJTANBgkqhkiG9w0BAQsFADBtMRMwEQYKCZImiZPyLGQB\n" +
            "GRYDY29tMRowGAYKCZImiZPyLGQBGRYKd2lubGxjLWRldjETMBEGCgmSJomT8ixk\n" +
            "ARkWA3BraTESMBAGCgmSJomT8ixkARkWAmNhMREwDwYDVQQDDAhXSU4gUk9PVDAe\n" +
            "Fw0yMDA0MjYxMzA4MjFaFw0yMjA0MTYxMzA4MjFaMCYxJDAiBgNVBAMMG2luZ3Jl\n" +
            "c3Mua3ViZS53aW5sbGMtZGV2LmNvbTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCC\n" +
            "AQoCggEBAMoI4iJlZAve6SZHBVL4mkmzLQ4NjyJTSx+tF9qAmh5/IP6c2bxqqKr2\n" +
            "aauNaYStU+7oWMxu7FMk/1atfWQ4ruZoSO9Eqx1sQqNr2obz23gHtAwWvOFxmmYQ\n" +
            "kvpJPPuuU8qUGpLKy1bQMLioptDLbnbbFcZcBJGbhdyJmyjxC9sOIOXqGrfBdpqw\n" +
            "dD6R3POL1CGchwO4C821x7ngGOqlfX/ysfuJsVsACYXiowHGvSBXsZt8gSb8EeFv\n" +
            "Kdcfziv4RGAqXB/jl4pF0WUcqXTo1ZdFtCi2KvLYOXD/Kdm2gMQ3GiCD7VfQb7bC\n" +
            "44vr4azxo0sC51sx6US/Jjidh+LbXqsCAwEAAaOBxTCBwjAfBgNVHSMEGDAWgBRG\n" +
            "EhS/MD8CvuMH3HIn1ytachRxuzAmBgNVHREEHzAdghtpbmdyZXNzLmt1YmUud2lu\n" +
            "bGxjLWRldi5jb20wSAYIKwYBBQUHAQEEPDA6MDgGCCsGAQUFBzABhixodHRwOi8v\n" +
            "ZG9ndGFnLWNhLndpbmxsYy1kZXYuY29tOjgwODAvY2Evb2NzcDAOBgNVHQ8BAf8E\n" +
            "BAMCBLAwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMCMA0GCSqGSIb3DQEB\n" +
            "CwUAA4IBAQBI5COMesYLsSxc2Tx54QtvzeNdecLqdboUpnVY842WcXCtI/CtvBhV\n" +
            "qKq4nCB7znpItuB7cgVn0Hxwtxr2w0wUfVtWxAklmj0Y3+sHFR3EG6zO3pbqPRT7\n" +
            "IBJvnvNLlmxMKy5zP1edn0DV/DFGJuBbMXOsVqw9xMNQj0IM9tIsjTT2tuU5AqVa\n" +
            "whrg05qNTuU3XRGc605eyzek0kXd6zrjaGS4YrN/9U533ncsEs1M+SIlpocvinRD\n" +
            "+2/vl1YfoDobxdSbWXYrgpxMBRYMbLcOwrXChT1v5FLYJqtpPEO4VkSQZkOy4vdR\n" +
            "JJjhv4LdCnyD/RT6lxXzMVzBqX5721Hu\n" +
            "-----END CERTIFICATE-----";

    public static final String testRootCa = "-----BEGIN CERTIFICATE-----\n" +
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

    @Override
    public CertAuthorityConnectionType getType() {
        return CertAuthorityConnectionType.INTERNAL;
    }

    @Override
    public List<String> getRequiredConnectionProperties() {
        return new ArrayList<>();
    }

    @Override
    public Map<String, String> getDefaultProperties() {
        return new HashMap<>();
    }

    @Override
    public String getName() {
        return "mockca";
    }

    @Override
    public X509Certificate issueCertificate(String csr, SubjectAltNames sans) throws Exception {
        return CertUtil.base64ToCert(testX509Cert);
    }

    @Override
    public boolean revokeCertificate(String serial, int reason) throws Exception {
        return true;
    }

    @Override
    public String getCertificateStatus(String serial) {
        return null;
    }

    @Override
    public List<CertificateDetails> search(CertSearchParam params) {
        if(params.getField() == CertSearchParams.CertField.SUBJECT && params.getValue().equals("test.winllc-dev.com")){
            try {
                CertificateDetails certificateDetails = new CertificateDetails(CertUtil.base64ToCert(testX509Cert));
                return Collections.singletonList(certificateDetails);
            } catch (CertificateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return new ArrayList<>();
    }

    @Override
    public Certificate[] getTrustChain() {
        return CertUtil.trustChainStringToCertArray(testRootCa);
    }

    @Override
    public X509Certificate getCertificateBySerial(String serial) throws Exception {
        return CertUtil.base64ToCert(testX509Cert);
    }
}
