package com.winllc.pki.ra.mock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.winllc.acme.common.model.AcmeJWSObject;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.UUID;

public class MockUtil {

    public static JWK rsaJWK;
    public static JWK alternateRsaJwk;
    public static JWK hmacJwk;
    private static SecretKey hmacKey;

    static {
        try {
            rsaJWK = new RSAKeyGenerator(2048)
                    .keyID("123")
                    .generate();

            alternateRsaJwk = new RSAKeyGenerator(2048)
                    .keyID("123")
                    .generate();

            hmacKey = KeyGenerator.getInstance("HmacSha256").generateKey();
            hmacJwk = new OctetSequenceKey.Builder(hmacKey)
                    .keyID(UUID.randomUUID().toString()) // give the key some ID (optional)
                    .algorithm(JWSAlgorithm.HS256) // indicate the intended key alg (optional)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static UserDetails generateUserDetails(){
        UserDetails userDetails =
                new org.springframework.security.core.userdetails.User("test@test.com", "", new ArrayList<>());
        return userDetails;
    }

    public static AcmeJWSObject buildCustomAcmeJwsObject(Object jsonObject, String url)
            throws JsonProcessingException, JOSEException, ParseException {
        ObjectMapper objectMapper = new ObjectMapper();
        JWSObject jwsObject = buildCustomJwsObject(objectMapper.writeValueAsString(jsonObject), url,
                "testkid1", JWSAlgorithm.RS256, false);
        String jsonString = jwsObjectAsString(jwsObject);
        return AcmeJWSObject.parse(jsonString);
    }

    public static JWSObject buildCustomJwsObject(String jsonPayload, String url, String kid,
                                                 JWSAlgorithm jwsAlgorithm, boolean hasNonce) throws JOSEException {

        JWSHeader.Builder builder;
        JWSSigner signer;

        if(JWSAlgorithm.Family.HMAC_SHA.contains(jwsAlgorithm)){
            builder = new JWSHeader.Builder(jwsAlgorithm)
                    .jwk(hmacJwk.toPublicJWK());
            signer = new MACSigner(hmacKey);
        }else{
            builder = new JWSHeader.Builder(jwsAlgorithm)
                    .jwk(rsaJWK.toPublicJWK());
            signer = new RSASSASigner((RSAKey) rsaJWK);
        }

        if(url != null){
            builder.customParam("url", url);
        }

        if(hasNonce) {
            builder.customParam("nonce", "1");
        }

        if(kid != null){
            builder.keyID(kid);
        }

        JWSHeader jwsHeader = builder.build();

        Payload payload = new Payload(jsonPayload);
        JWSObject jwsObject = new JWSObject(jwsHeader, payload);

        jwsObject.sign(signer);

        return jwsObject;
    }

    public static String jwsObjectAsString(JWSObject jwsObject){
        return "{\"protected\":\"" + jwsObject.getHeader().toBase64URL() + "\"," +
                " \"payload\":\"" + jwsObject.getPayload().toBase64URL() + "\", \"signature\":\"" + jwsObject.getSignature() + "\"}";
    }
}
