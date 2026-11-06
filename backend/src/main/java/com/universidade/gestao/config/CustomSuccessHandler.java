package com.universidade.gestao.config;

import java.io.IOException;
import java.util.Collection;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomSuccessHandler implements AuthenticationSuccessHandler {

    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // Se o usuário tentou acessar uma página específica antes do login (ex: /matricula), volta para ela
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null) {
            response.sendRedirect(savedRequest.getRedirectUrl());
            return;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();

            if (role.equals("ROLE_ALUNO")) {
                response.sendRedirect("/aluno/dashboard");
                return;
            } else if (role.equals("ROLE_PROFESSOR")) {
                response.sendRedirect("/professor/dashboard");
                return;
            } else if (role.equals("ROLE_COORDENADOR")) {
                response.sendRedirect("/coordenador/dashboard");
                return;
            } else if (role.equals("ROLE_ATENDENTE")) {
                response.sendRedirect("/atendente/dashboard");
                return;
            }
        }

        response.sendRedirect("/");
    }
}
