package com.example.crud.config;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.core.convert.converter.Converter;

public class JwtAccountConverter implements Converter<Jwt, JwtAuthenticationToken> {
    private final UserDetailsService accounts;

    public JwtAccountConverter(UserDetailsService accounts) {
        this.accounts = accounts;
    }

    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) throw new BadCredentialsException("Token inválido");
        org.springframework.security.core.userdetails.UserDetails account;
        try {
            account = accounts.loadUserByUsername(subject);
        } catch (UsernameNotFoundException ex) {
            throw new BadCredentialsException("Conta indisponível");
        }
        if (!account.isEnabled() || !account.isAccountNonLocked() || !account.isAccountNonExpired()
                || !account.isCredentialsNonExpired()) {
            throw new BadCredentialsException("Conta indisponível");
        }
        return new JwtAuthenticationToken(jwt, account.getAuthorities(), account.getUsername());
    }
}

