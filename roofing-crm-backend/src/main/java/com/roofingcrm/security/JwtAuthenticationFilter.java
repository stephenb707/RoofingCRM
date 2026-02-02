package com.roofingcrm.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        boolean hasAuth = StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ");
        log.debug("JwtAuthenticationFilter: path={} hasAuthHeader={}", path, hasAuth);

        if (hasAuth) {
            String token = authHeader.substring(7);

            try {
                AuthenticatedUser authUser = jwtService.parseToken(token);
                AbstractAuthenticationToken authentication = new AbstractAuthenticationToken(
                        AuthorityUtils.NO_AUTHORITIES) {

                    @Override
                    public Object getCredentials() {
                        return token;
                    }

                    @Override
                    public Object getPrincipal() {
                        return authUser;
                    }
                };
                authentication.setAuthenticated(true);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ex) {
                log.debug("JwtAuthenticationFilter: token parse failed path={} reason={}", path, ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
        if (log.isDebugEnabled() && path != null && path.contains("schedule")) {
            var ctx = SecurityContextHolder.getContext().getAuthentication();
            log.debug("JwtAuthenticationFilter after chain: path={} authSet={}", path, ctx != null && ctx.isAuthenticated());
        }
    }
}
