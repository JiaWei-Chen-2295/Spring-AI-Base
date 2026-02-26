package com.example.aitemplate.infra.security;

import com.example.aitemplate.core.user.User;
import com.example.aitemplate.infra.db.UserRepository;
import java.util.Collections;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        if (!user.isEnabled()) {
            throw new UsernameNotFoundException("用户已禁用: " + username);
        }

        var authorities = user.roles() != null
                ? user.roles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.roleCode()))
                        .collect(java.util.stream.Collectors.toList())
                : Collections.<SimpleGrantedAuthority>emptyList();

        return new org.springframework.security.core.userdetails.User(
                user.username(),
                user.password(),
                user.isEnabled(),
                true,
                true,
                true,
                authorities
        );
    }
}
