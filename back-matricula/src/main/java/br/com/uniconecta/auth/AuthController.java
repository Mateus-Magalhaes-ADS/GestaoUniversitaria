package br.com.uniconecta.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.uniconecta.aluno.Aluno;
import br.com.uniconecta.aluno.AlunoRepository;
import br.com.uniconecta.aluno.AlunoResponse;
import br.com.uniconecta.common.ApiException;
import br.com.uniconecta.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AlunoRepository alunoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(AlunoRepository alunoRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.alunoRepository = alunoRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public record LoginRequest(
            @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") String email,
            @NotBlank(message = "Informe a senha.") String senha) {}

    public record LoginResponse(String token, AlunoResponse aluno) {}

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Aluno aluno = alunoRepository.findByEmailIgnoreCase(request.email().trim())
                .filter(a -> passwordEncoder.matches(request.senha(), a.getSenhaHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos."));
        return new LoginResponse(jwtService.gerarToken(aluno), AlunoResponse.de(aluno));
    }

    @GetMapping("/me")
    public AlunoResponse me(@AuthenticationPrincipal Long alunoId) {
        return alunoRepository.findById(alunoId)
                .map(AlunoResponse::de)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Aluno não encontrado."));
    }
}
