package com.universidade.gestao.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.universidade.gestao.model.Matricula;
import com.universidade.gestao.model.Usuario;
import com.universidade.gestao.repository.MatriculaRepository;
import com.universidade.gestao.repository.UsuarioRepository;

@Controller
@RequestMapping("/matricula")
public class MatriculaController {

    private final MatriculaRepository matriculaRepository;
    private final UsuarioRepository usuarioRepository;

    public MatriculaController(MatriculaRepository matriculaRepository, UsuarioRepository usuarioRepository) {
        this.matriculaRepository = matriculaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public String exibirFormularioMatricula(@AuthenticationPrincipal UserDetails user, Model model) {
        model.addAttribute("tituloPage", "Solicitação de Matrícula");

        // Tratamento seguro caso o usuário não esteja logado
        if (user != null) {
            usuarioRepository.findByEmail(user.getUsername()).ifPresent(usuario -> {
                model.addAttribute("nomeAluno", usuario.getNome());
                model.addAttribute("emailAluno", usuario.getEmail());
            });
        }

        // Aponta para templates/matricula.html
        return "matricula";
    }

    @PostMapping("/enviar")
    public String processarMatricula(
            @RequestParam("cpf") String cpf,
            @RequestParam("endereco") String endereco,
            @RequestParam("cep") String cep,
            @RequestParam("cursoId") Long cursoId,
            @RequestParam("documentoIdentidade") MultipartFile documentoIdentidade,
            @RequestParam("historicoEscolar") MultipartFile historicoEscolar,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes redirectAttributes) {

        if (user == null) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Você precisa estar autenticado para solicitar a matrícula.");
            return "redirect:/login";
        }

        String cpfLimpo = cpf.replaceAll("[^0-9]", "");

        // Validação de duplicidade usando o repository
        if (matriculaRepository.existsByCpf(cpfLimpo)) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Já existe uma solicitação de matrícula pendente para este CPF.");
            return "redirect:/matricula";
        }

        try {
            Usuario aluno = usuarioRepository.findByEmail(user.getUsername())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

            Matricula matricula = new Matricula(aluno.getNome(), aluno.getEmail(), cpfLimpo, cursoId);
            matricula.setEndereco(endereco.trim());
            matricula.setCep(cep.trim());

            armazenarDocumento(documentoIdentidade, true, matricula);
            armazenarDocumento(historicoEscolar, false, matricula);

            matriculaRepository.save(matricula);

            redirectAttributes.addFlashAttribute("mensagemSucesso", "Solicitação de matrícula enviada com sucesso!");
        } catch (IOException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
            return "redirect:/matricula";
        }

        return "redirect:/matricula";
    }

    @GetMapping("/{id}/documento/{tipo}")
    public ResponseEntity<byte[]> baixarDocumento(
            @PathVariable Long id,
            @PathVariable String tipo,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        Matricula matricula = matriculaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        boolean equipe = user.getAuthorities().stream().anyMatch(authority ->
                authority.getAuthority().equals("ROLE_ATENDENTE")
                        || authority.getAuthority().equals("ROLE_COORDENADOR"));

        if (!equipe && !normalizeEmail(matricula.getEmail()).equals(normalizeEmail(user.getUsername()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        String filename;
        String contentType;
        byte[] content;

        if ("identidade".equals(tipo)) {
            filename = matricula.getDocumentoIdentidadeNome();
            contentType = matricula.getDocumentoIdentidadeContentType();
            content = matricula.getDocumentoIdentidadeConteudo();
        } else if ("historico".equals(tipo)) {
            filename = matricula.getHistoricoEscolarNome();
            contentType = matricula.getHistoricoEscolarContentType();
            content = matricula.getHistoricoEscolarConteudo();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        if (content == null || filename == null || contentType == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());
        headers.setContentLength(content.length);

        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }

    private void armazenarDocumento(MultipartFile arquivo, boolean identidade, Matricula matricula) throws IOException {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Os dois documentos precisam ser anexados.");
        }

        String nome = arquivo.getOriginalFilename();
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O arquivo enviado precisa ter um nome válido.");
        }

        String extensao = !nome.contains(".") ? "" : nome.substring(nome.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        String contentType = switch (extensao) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            default -> throw new IllegalArgumentException("Envie documentos em PDF, PNG, JPG ou JPEG.");
        };

        String nomeSeguro = nome.replaceAll("[\\\\/]", "_").replaceAll("[\\r\\n]", "_");

        if (identidade) {
            matricula.setDocumentoIdentidadeNome(nomeSeguro);
            matricula.setDocumentoIdentidadeContentType(contentType);
            matricula.setDocumentoIdentidadeConteudo(arquivo.getBytes());
        } else {
            matricula.setHistoricoEscolarNome(nomeSeguro);
            matricula.setHistoricoEscolarContentType(contentType);
            matricula.setHistoricoEscolarConteudo(arquivo.getBytes());
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
