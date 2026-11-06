package com.universidade.gestao.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.universidade.gestao.model.Role;
import com.universidade.gestao.model.Usuario;
import com.universidade.gestao.repository.UsuarioRepository;

@Controller
public class AutenticacaoController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String professorRegistrationCode;
    private final String coordinatorRegistrationCode;

    public AutenticacaoController(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${APP_PROFESSOR_REGISTRATION_CODE:}") String professorRegistrationCode,
            @Value("${APP_COORDINATOR_REGISTRATION_CODE:}") String coordinatorRegistrationCode) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.professorRegistrationCode = professorRegistrationCode;
        this.coordinatorRegistrationCode = coordinatorRegistrationCode;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/registrar")
    public String registrarForm(Model model) {
        return "registrar";
    }

    @PostMapping("/registrar")
    public String registrarUsuario(@RequestParam String nome,
                                   @RequestParam String email,
                                   @RequestParam String senha,
                                   @RequestParam(defaultValue = "ALUNO") String perfil,
                                   @RequestParam(required = false, defaultValue = "") String codigoConvite,
                                   RedirectAttributes redirectAttributes) {

        String emailNormalizado = (email != null) ? email.trim().toLowerCase(Locale.ROOT) : "";
        String perfilNormalizado = (perfil != null) ? perfil.trim().toUpperCase(Locale.ROOT) : "ALUNO";

        redirectAttributes.addFlashAttribute("nomeInformado", nome);
        redirectAttributes.addFlashAttribute("emailInformado", emailNormalizado);
        redirectAttributes.addFlashAttribute("perfilInformado", perfilNormalizado);

        if (nome == null || nome.isBlank() || emailNormalizado.isBlank() || senha == null || senha.isBlank()) {
            return "redirect:/registrar?erro=campos_validacao";
        }

        Role role;
        try {
            role = Role.valueOf(perfilNormalizado);
        } catch (IllegalArgumentException exception) {
            return "redirect:/registrar?erro=perfil";
        }

        if (role == Role.ATENDENTE || (role != Role.ALUNO && !conviteValido(role, codigoConvite))) {
            return "redirect:/registrar?erro=convite";
        }

        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            return "redirect:/registrar?erro=email";
        }

        String senhaEncriptada = passwordEncoder.encode(senha);
        Usuario usuario = new Usuario(nome.trim(), emailNormalizado, senhaEncriptada, role);
        usuarioRepository.save(usuario);

        return "redirect:/login?sucesso";
    }

    private boolean conviteValido(Role role, String codigoConvite) {
        String codigoConfigurado = switch (role) {
            case PROFESSOR -> professorRegistrationCode;
            case COORDENADOR -> coordinatorRegistrationCode;
            default -> "";
        };
        String codigoConfiguradoNormalizado = normalizarCodigoConvite(codigoConfigurado);
        String codigoConviteNormalizado = normalizarCodigoConvite(codigoConvite);
        if (codigoConfiguradoNormalizado.isBlank() || codigoConviteNormalizado.isBlank()) {
            return false;
        }

        return MessageDigest.isEqual(
                codigoConfiguradoNormalizado.toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                codigoConviteNormalizado.toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }

    private String normalizarCodigoConvite(String codigo) {
        return codigo == null ? "" : codigo.trim();
    }
}
