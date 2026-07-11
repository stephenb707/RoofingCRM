package com.roofingcrm.realtime;

import com.roofingcrm.security.AuthenticatedUser;
import com.roofingcrm.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class JwtHandshakeInterceptorTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private WebSocketHandler webSocketHandler;

    @Test
    void beforeHandshake_validTokenStoresAuthenticatedUser() {
        AuthenticatedUser authUser =
                new AuthenticatedUser(UUID.randomUUID(), "member@example.com");
        Map<String, Object> attributes = new HashMap<>();
        when(request.getURI()).thenReturn(URI.create("https://example.test/ws?token=valid-token"));
        when(jwtService.parseToken("valid-token")).thenReturn(authUser);

        JwtHandshakeInterceptor interceptor = new JwtHandshakeInterceptor(jwtService);

        assertTrue(interceptor.beforeHandshake(request, response, webSocketHandler, attributes));
        assertSame(authUser, attributes.get(JwtHandshakeInterceptor.AUTH_USER_ATTRIBUTE));
    }
}
