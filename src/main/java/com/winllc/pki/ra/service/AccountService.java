package com.winllc.pki.ra.service;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.Base64URL;
import com.winllc.pki.ra.beans.AccountRequestForm;
import com.winllc.pki.ra.beans.AccountUpdateForm;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.util.AppUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.Base64;

@RestController
@RequestMapping("/account")
public class AccountService {

    private static final Logger log = LogManager.getLogger(AccountService.class);

    static String macKey = "2798044239550a105ef8ba7f187c3c0b657dda5a1aa9500dd0956d943bd4e94501961bf84ecc3578c90aa09b5578b5d313a744e48fe7ecf60d20f0ae6d3ebc5e";

    @Autowired
    private AccountRepository accountRepository;

    static {
        System.out.println("System MAC key: "+macKey);
    }

    @PostConstruct
    private void postConstruct(){
        Account testAccount = new Account();
        testAccount.setKeyIdentifier("kidtest");
        testAccount.setMacKey(macKey);

        accountRepository.save(testAccount);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createNewAccount(@RequestBody AccountRequestForm form){
        String macKey = AppUtil.generate256BitKey();
        String keyIdentifier = "TODO";

        //TODO return both to account holder for entry into ACME client

        return ResponseEntity.status(201).build();
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateAccount(AccountUpdateForm form){
        //TODO
        if(form.isValid()){

        }

        return ResponseEntity.status(200).build();
    }

    @GetMapping("/findByKeyIdentifier/{kid}")
    public ResponseEntity<?> findByKeyIdentifier(@PathVariable String kid){

        Account account = accountRepository.findByKeyIdentifierEquals(kid);

        return ResponseEntity.ok(account);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyExternalAccountBinding(@RequestParam String macKey, @RequestParam String keyIdentifier,
                                                          @RequestParam String jwsObject, @RequestParam String accountObject){
        Base64URL macKeyBase64 = new Base64URL(macKey);

        System.out.println("MAC Key: "+ macKeyBase64.toString());
        System.out.println("Key Identifier: "+keyIdentifier);

        try {
            Account account = accountRepository.findByKeyIdentifierEquals(keyIdentifier);

            JWSObject jwsObjectParsed = JWSObject.parse(jwsObject);
            JWSObject accountJWSParsed = JWSObject.parse(accountObject);

            try {
                JWSSigner signer = new MACSigner(account.getMacKey());

                JWSObject testObj = new JWSObject(jwsObjectParsed.getHeader(), jwsObjectParsed.getPayload());
                testObj.sign(signer);

                System.out.println("Test signed obj: "+testObj.getSignature().toJSONString());

                if(testObj.getSignature().toString().contentEquals(jwsObjectParsed.getSignature().toString())){
                    System.out.println("Account request verified!");

                    //account.setMacKey(jwsObjectParsed.getSignature().toString());
                    //account.setKeyIdentifier(jwsObjectParsed.getHeader().getKeyID());

                    //accountRepository.save(account);

                    return ResponseEntity.status(200)
                            .build();
                }
            } catch (KeyLengthException e) {
                e.printStackTrace();
            }


        } catch (Exception e) {
            log.error("Could not verify request", e);
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
