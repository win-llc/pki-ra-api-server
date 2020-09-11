package com.winllc.pki.ra.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/** Validator for expected audience in access tokens. */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String jwtAudienceRequired;

    public AudienceValidator(String jwtAudienceRequired) {
        this.jwtAudienceRequired = jwtAudienceRequired;
    }

    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (jwt.getAudience().contains(jwtAudienceRequired)) {
            return OAuth2TokenValidatorResult.success();
        } else {
            OAuth2Error error =
                new OAuth2Error("invalid_token", "The required audience '"+jwtAudienceRequired+"' is missing", null);
            return OAuth2TokenValidatorResult.failure(error);
        }
    }
}