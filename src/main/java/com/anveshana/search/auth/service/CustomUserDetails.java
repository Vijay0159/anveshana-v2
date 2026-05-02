package com.anveshana.search.auth.service;

import com.anveshana.search.auth.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

public class CustomUserDetails implements UserDetails {

    private final User domainUser;

    public CustomUserDetails(User domainUser) {
        this.domainUser = domainUser;
    }

    public User getDomainUser() {
        return domainUser;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return domainUser.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    @Override public String getPassword()  { return domainUser.getPassword(); }
    @Override public String getUsername()  { return domainUser.getEmail(); }
    @Override public boolean isEnabled()   { return domainUser.isEnabled(); }
    @Override public boolean isAccountNonExpired()    { return true; }
    @Override public boolean isAccountNonLocked()     { return true; }
    @Override public boolean isCredentialsNonExpired(){ return true; }
}
