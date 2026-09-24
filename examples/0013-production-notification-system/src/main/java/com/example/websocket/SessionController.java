package com.example.websocket;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contexts = new HttpSessionSecurityContextRepository();

    public SessionController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/api/session/login")
    public Map<String, String> login(@RequestBody TokenController.LoginRequest request,
                                     HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        // Rotate the anonymous/pre-authentication session before persisting the
        // authenticated context. Spring Session keeps the new id shared across
        // application instances.
        if (httpRequest.getSession(false) != null) {
            httpRequest.changeSessionId();
        }
        contexts.saveContext(context, httpRequest, httpResponse);
        return Map.of("user", authentication.getName(), "mode", "session");
    }

    @PostMapping("/api/session/logout")
    public Map<String, Boolean> logout(HttpServletRequest request) throws Exception {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return Map.of("loggedOut", true);
    }
}
