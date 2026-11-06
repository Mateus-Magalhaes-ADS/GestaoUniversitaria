package com.universidade.gestao.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.universidade.gestao.model.Usuario;
import com.universidade.gestao.repository.MatriculaRepository;
import com.universidade.gestao.repository.UsuarioRepository;

@Controller
public class DashboardController {

    private final UsuarioRepository usuarioRepository;
    private final MatriculaRepository matriculaRepository;

    public DashboardController(UsuarioRepository usuarioRepository, MatriculaRepository matriculaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.matriculaRepository = matriculaRepository;
    }

    @GetMapping("/aluno/dashboard")
    public String dashboardAluno(@AuthenticationPrincipal UserDetails user, Model model) {
        adicionarUsuario(model, user);
        model.addAttribute("matriculas", matriculaRepository.findAllByEmail(user.getUsername()));
        return "dashboards/aluno";
    }

    @GetMapping("/professor/dashboard")
    public String dashboardProfessor(@AuthenticationPrincipal UserDetails user, Model model) {
        adicionarUsuario(model, user);
        return "dashboards/professor";
    }

    @GetMapping("/coordenador/dashboard")
    public String dashboardCoordenador(@AuthenticationPrincipal UserDetails user, Model model) {
        adicionarUsuario(model, user);
        model.addAttribute("matriculas", matriculaRepository.findAll());
        return "dashboards/coordenador";
    }

    @GetMapping("/atendente/dashboard")
    public String dashboardAtendente(@AuthenticationPrincipal UserDetails user, Model model) {
        adicionarUsuario(model, user);
        model.addAttribute("matriculas", matriculaRepository.findAll());
        return "dashboards/atendente";
    }

    private void adicionarUsuario(Model model, UserDetails user) {
        Usuario usuario = usuarioRepository.findByEmail(user.getUsername()).orElse(null);
        model.addAttribute("usuario", usuario == null ? user.getUsername() : usuario.getNome());
        model.addAttribute("emailUsuario", user.getUsername());
    }
}
