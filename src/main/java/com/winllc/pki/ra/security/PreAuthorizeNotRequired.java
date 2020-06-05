package com.winllc.pki.ra.security;

import java.lang.annotation.*;

/**
 * Custom annotation without any function but just to document that no pre-authorization is required
 * here by intention.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface PreAuthorizeNotRequired {}