package com.studywithus.identity.infrastructure;

import com.studywithus.identity.domain.GlobalRole;
import com.studywithus.identity.domain.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
public class SecurityUserPrincipal implements UserDetails {

    private final UUID id;
    private final String username;
    private final String email;
    private final String password;
    private final boolean active;
    private final GlobalRole globalRole;
    private final Collection<? extends GrantedAuthority> authorities;

    public SecurityUserPrincipal(User user, String passwordHash) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.password = passwordHash;
        this.active = user.isActive();
        this.globalRole = user.getEffectiveGlobalRole();
        this.authorities = buildAuthorities(this.globalRole);
    }

    public SecurityUserPrincipal(UUID id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = "";
        this.active = true;
        this.globalRole = GlobalRole.USER;
        this.authorities = buildAuthorities(GlobalRole.USER);
    }

    private static Collection<? extends GrantedAuthority> buildAuthorities(GlobalRole role) {
        if (role == null || role == GlobalRole.USER) {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + role.name()),
                new SimpleGrantedAuthority("ROLE_USER")
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return active;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return active;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
