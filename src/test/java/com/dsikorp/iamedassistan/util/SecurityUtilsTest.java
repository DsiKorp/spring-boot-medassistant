package com.dsikorp.iamedassistan.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

/**
 * Tests unitarios de {@link SecurityUtils}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityUtils")
class SecurityUtilsTest {

    @Mock
    private Authentication authentication;

    private final SecurityUtils securityUtils = new SecurityUtils();

    private static GrantedAuthority authority(String name) {
        return new SimpleGrantedAuthority(name);
    }

    @Test
    @DisplayName("extractRole devuelve ADMIN cuando la única autoridad es ROLE_ADMIN")
    void extractRoleReturnsAdminForRoleAdmin() {

        doReturn(List.of(authority("ROLE_ADMIN"))).when(authentication).getAuthorities();

        assertThat(securityUtils.extractRole(authentication)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("extractRole devuelve PATIENT cuando la única autoridad es ROLE_PATIENT")
    void extractRoleReturnsPatientForRolePatient() {

        doReturn(List.of(authority("ROLE_PATIENT"))).when(authentication).getAuthorities();

        assertThat(securityUtils.extractRole(authentication)).isEqualTo("PATIENT");
    }

    @Test
    @DisplayName("extractRole devuelve UNKNOWN cuando ninguna autoridad es ADMIN ni PATIENT")
    void extractRoleReturnsUnknownForUnrelatedAuthority() {

        doReturn(List.of(authority("ROLE_OTHER"))).when(authentication).getAuthorities();

        assertThat(securityUtils.extractRole(authentication)).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("extractRole devuelve UNKNOWN cuando la autenticación no tiene autoridades")
    void extractRoleReturnsUnknownForEmptyAuthorities() {

        doReturn(List.of()).when(authentication).getAuthorities();

        assertThat(securityUtils.extractRole(authentication)).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("extractRole devuelve la primera autoridad válida en orden de iteración")
    void extractRoleReturnsTheFirstMatchingAuthorityInIterationOrder() {

        doReturn(List.of(authority("ROLE_PATIENT"), authority("ROLE_ADMIN")))
                .when(authentication).getAuthorities();

        assertThat(securityUtils.extractRole(authentication)).isEqualTo("PATIENT");
    }
}
