package com.universidade.gestao.controller.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.universidade.gestao.model.Matricula;
import com.universidade.gestao.model.Role;
import com.universidade.gestao.model.Usuario;
import com.universidade.gestao.repository.MatriculaRepository;
import com.universidade.gestao.repository.UsuarioRepository;

@RestController
@RequestMapping("/api/v1/matriculas")
public class MatriculaApiController {

    private final MatriculaRepository matriculas;
    private final UsuarioRepository usuarios;

    public MatriculaApiController(MatriculaRepository matriculas, UsuarioRepository usuarios) {
        this.matriculas = matriculas;
        this.usuarios = usuarios;
    }

    @GetMapping
    public List<EnrollmentResponse> list(@AuthenticationPrincipal Jwt jwt) {
        Role role = currentRole(jwt);
        List<Matricula> results = role == Role.ALUNO
                ? matriculas.findAllByEmail(jwt.getSubject())
                : matriculas.findAll();
        return results.stream().map(EnrollmentResponse::from).toList();
    }

    @GetMapping("/{id}")
    public EnrollmentResponse get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Matricula matricula = matriculas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Role role = currentRole(jwt);
        if (role == Role.ALUNO && !sameEmail(matricula.getEmail(), jwt.getSubject())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return EnrollmentResponse.from(matricula);
    }

    @GetMapping("/{id}/documentos/{tipo}")
    public ResponseEntity<byte[]> download(
            @PathVariable Long id,
            @PathVariable String tipo,
            @AuthenticationPrincipal Jwt jwt) {
        Matricula matricula = matriculas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Role role = currentRole(jwt);
        if (role == Role.ALUNO && !sameEmail(matricula.getEmail(), jwt.getSubject())) {
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
        if (filename == null || contentType == null || content == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8).build());
        headers.setContentLength(content.length);
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EnrollmentResponse create(
            @RequestParam String cpf,
            @RequestParam String endereco,
            @RequestParam String cep,
            @RequestParam Long cursoId,
            @RequestParam MultipartFile documentoIdentidade,
            @RequestParam MultipartFile historicoEscolar,
            @AuthenticationPrincipal Jwt jwt) {
        Usuario aluno = usuarios.findByEmail(jwt.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        String cleanCpf = cpf.replaceAll("[^0-9]", "");
        if (matriculas.existsByCpf(cleanCpf)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma solicitação para este CPF.");
        }

        Matricula matricula = new Matricula(aluno.getNome(), aluno.getEmail(), cleanCpf, cursoId);
        matricula.setEndereco(endereco.trim());
        matricula.setCep(cep.trim());
        try {
            storeDocument(documentoIdentidade, true, matricula);
            storeDocument(historicoEscolar, false, matricula);
        } catch (IOException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
        return EnrollmentResponse.from(matriculas.save(matricula));
    }

    private Role currentRole(Jwt jwt) {
        Usuario usuario = usuarios.findByEmail(jwt.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return usuario.getPerfil();
    }

    private boolean sameEmail(String first, String second) {
        return first != null && second != null
                && first.trim().toLowerCase(Locale.ROOT).equals(second.trim().toLowerCase(Locale.ROOT));
    }

    private void storeDocument(MultipartFile file, boolean identity, Matricula matricula) throws IOException {
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null) {
            throw new IllegalArgumentException("Os dois documentos precisam ser anexados.");
        }
        String filename = file.getOriginalFilename().replaceAll("[\\\\/\\r\\n]", "_");
        String extension = filename.contains(".")
                ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        String contentType = switch (extension) {
            case "pdf" -> MediaType.APPLICATION_PDF_VALUE;
            case "png" -> MediaType.IMAGE_PNG_VALUE;
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG_VALUE;
            default -> throw new IllegalArgumentException("Envie PDF, PNG, JPG ou JPEG.");
        };
        if (identity) {
            matricula.setDocumentoIdentidadeNome(filename);
            matricula.setDocumentoIdentidadeContentType(contentType);
            matricula.setDocumentoIdentidadeConteudo(file.getBytes());
        } else {
            matricula.setHistoricoEscolarNome(filename);
            matricula.setHistoricoEscolarContentType(contentType);
            matricula.setHistoricoEscolarConteudo(file.getBytes());
        }
    }

    public record EnrollmentResponse(Long id, String nomeCompleto, String email, String cpf,
            String endereco, String cep, Long cursoId, String dataSolicitacao,
            String documentoIdentidadeNome, String historicoEscolarNome) {
        static EnrollmentResponse from(Matricula matricula) {
            return new EnrollmentResponse(matricula.getId(), matricula.getNomeCompleto(), matricula.getEmail(),
                    matricula.getCpf(), matricula.getEndereco(), matricula.getCep(), matricula.getCursoId(),
                    matricula.getDataSolicitacao().toString(), matricula.getDocumentoIdentidadeNome(),
                    matricula.getHistoricoEscolarNome());
        }
    }
}
