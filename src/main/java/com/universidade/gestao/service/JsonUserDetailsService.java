package com.universidade.gestao.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.universidade.gestao.repository.UsuarioRepository;

@Service
public class JsonUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public JsonUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return usuarioRepository.findByEmail(email)
                .map(usuario -> User.withUsername(usuario.getEmail())
                        .password(usuario.getSenha())
                        .roles(usuario.getPerfil().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Conta não encontrada."));
    }
}
