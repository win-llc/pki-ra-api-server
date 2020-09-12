package com.winllc.pki.ra.service;

import org.springframework.context.ApplicationContext;

public abstract class AbstractService {
    protected final ApplicationContext context;

    protected AbstractService(ApplicationContext context){
        this.context = context;
    }
}
