package com.salesmanager.shop.store.security;

import com.salesmanager.shop.model.entity.Entity;

import java.io.Serializable;

public class AuthenticationResponse extends Entity implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private String token;
    public AuthenticationResponse() {
    }

    public AuthenticationResponse(Long userId, String token) {
        this.token = token;
        super.setId(userId);
    }

    public String getToken() {
        return token;
    }

}
