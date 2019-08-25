package com.winllc.pki.ra.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.util.Base64URL;
import com.winllc.pki.ra.domain.Account;
import org.jose4j.jwk.PublicJsonWebKey;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/account")
public class AccountService {

    static String macKey = "2798044239550a105ef8ba7f187c3c0b657dda5a1aa9500dd0956d943bd4e94501961bf84ecc3578c90aa09b5578b5d313a744e48fe7ecf60d20f0ae6d3ebc5e";
    static String macKeyB64 = Base64.getEncoder().encodeToString(macKey.getBytes());

    private static Map<String, Account> accountMap;

    static {
        System.out.println("System MAC key: "+macKey);
        System.out.println("System MAC key B64: "+macKeyB64);

        Account testAccount = new Account();
        testAccount.setKeyIdentifier("kidtest");
        testAccount.setMacKey(macKey);

        accountMap = new HashMap<>();
        accountMap.put(testAccount.getKeyIdentifier(), testAccount);
    }

    public void createNewAccount(String accountName){
        String macKey = "TODO";
        String keyIdentifier = "TODO";

        //TODO return both to account holder for entry into ACME client
    }

    public Account findByKeyIdentifier(String kid){
        //TODO

        return accountMap.get(kid);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyExternalAccountBinding(@RequestParam String macKey, @RequestParam String keyIdentifier,
                                                          @RequestParam String jwsObject, @RequestParam String accountObject){
        Base64URL macKeyBase64 = new Base64URL(macKey);

        System.out.println("MAC Key: "+ macKeyBase64.toString());
        System.out.println("Key Identifier: "+keyIdentifier);

        try {
            Account account = findByKeyIdentifier(keyIdentifier);

            JWSObject jwsObjectParsed = JWSObject.parse(jwsObject);
            JWSObject accountJWSParsed = JWSObject.parse(accountObject);

            try {
                JWSSigner signer = new MACSigner(account.getMacKey());

                JWSObject testObj = new JWSObject(jwsObjectParsed.getHeader(), jwsObjectParsed.getPayload());
                testObj.sign(signer);

                System.out.println("Test signed obj: "+testObj.getSignature().toJSONString());

                if(testObj.getSignature().toString().contentEquals(jwsObjectParsed.getSignature().toString())){
                    System.out.println("Account request verified!");
                    return ResponseEntity.status(200)
                            .build();
                }
            } catch (KeyLengthException e) {
                e.printStackTrace();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        //TODO verify account
        return ResponseEntity.status(403)
                .build();
    }


    private boolean verifyBinding(Account systemAccount, JWSObject accountObject, JWSObject verifyObject){
        JWK accountPublicKey = accountObject.getHeader().getJWK().toPublicJWK();
        JWSHeader verifyHeader = verifyObject.getHeader();

        return false;
    }

}
