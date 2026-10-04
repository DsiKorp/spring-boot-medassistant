package com.dsikorp.iamedassistan.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    public String extractRole(Authentication authentication){
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter( a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_PATIENT"))
                .findFirst()
                .map(a -> a.replace("ROLE_", ""))
                .orElse("UNKNOWN");
    }
}
