package br.com.raizesdonordeste_api.seguranca;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.raizesdonordeste_api.cliente.Cliente;
import br.com.raizesdonordeste_api.cliente.ClienteRepository;
import br.com.raizesdonordeste_api.comum.CredenciaisInvalidasException;
import br.com.raizesdonordeste_api.usuario.Usuario;
import br.com.raizesdonordeste_api.usuario.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Login de usuários internos e clientes (RF01)")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthController(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
            PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    @Transactional(readOnly = true)
    @Operation(summary = "Login de usuário interno (atendente, cozinha, gerente, matriz)")
    public TokenResponse login(@Valid @RequestBody LoginRequest requisicao) {
        Usuario usuario = usuarioRepository.buscarPorEmail(normalizar(requisicao.email()))
                .filter(Usuario::isAtivo)
                .filter(u -> passwordEncoder.matches(requisicao.senha(), u.getSenhaHash()))
                .orElseThrow(CredenciaisInvalidasException::new);
        String papel = usuario.getPerfil().name();
        String token = tokenService.gerar(usuario.getEmail(), usuario.getId(), UsuarioLogado.TIPO_USUARIO, papel,
                usuario.getUnidadeId());
        return new TokenResponse(token, "Bearer", tokenService.validadeSegundos(), usuario.getNome(), papel,
                usuario.getUnidadeId());
    }

    @PostMapping("/clientes/login")
    @Transactional(readOnly = true)
    @Operation(summary = "Login de cliente do programa de fidelidade")
    public TokenResponse loginCliente(@Valid @RequestBody LoginRequest requisicao) {
        Cliente cliente = clienteRepository.findByEmail(normalizar(requisicao.email()))
                .filter(c -> !c.isAnonimizado())
                .filter(c -> passwordEncoder.matches(requisicao.senha(), c.getSenhaHash()))
                .orElseThrow(CredenciaisInvalidasException::new);
        String token = tokenService.gerar(cliente.getEmail(), cliente.getId(), UsuarioLogado.TIPO_CLIENTE,
                "CLIENTE", null);
        return new TokenResponse(token, "Bearer", tokenService.validadeSegundos(), cliente.getNome(), "CLIENTE",
                null);
    }

    private static String normalizar(String email) {
        return email.trim().toLowerCase();
    }
}
