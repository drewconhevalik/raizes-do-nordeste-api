package br.com.raizesdonordeste_api.seguranca;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Configuração de segurança: API sem estado de sessão, autenticação por token JWT e
 * autorização por perfil (RNF01). As regras finas por perfil ficam nos controllers
 * com @PreAuthorize; aqui ficam apenas as rotas públicas.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filtroDeSeguranca(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(rotas -> rotas
                .requestMatchers("/api/v1/auth/**", "/actuator/health", "/actuator/health/**",
                        "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/error")
                    .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/unidades", "/api/v1/unidades/*/cardapio",
                        "/api/v1/pedidos/acompanhamento/*")
                    .permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/clientes", "/api/v1/pedidos",
                        "/api/v1/pedidos/*/pagamentos", "/api/v1/pagamentos/webhook")
                    .permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(recurso -> recurso
                .authenticationEntryPoint(naoAutenticado())
                .accessDeniedHandler(acessoNegado())
                .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDePapeis())))
            .exceptionHandling(erros -> erros
                .authenticationEntryPoint(naoAutenticado())
                .accessDeniedHandler(acessoNegado()));
        return http.build();
    }

    /** Lê a lista "roles" do token e cria as autoridades ROLE_GERENTE, ROLE_CLIENTE etc. */
    @Bean
    JwtAuthenticationConverter conversorDePapeis() {
        JwtGrantedAuthoritiesConverter papeis = new JwtGrantedAuthoritiesConverter();
        papeis.setAuthoritiesClaimName("roles");
        papeis.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(papeis);
        return conversor;
    }

    @Bean
    SecretKey chaveJwt(@Value("${app.jwt.segredo}") String segredo) {
        return new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey chaveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey chaveJwt) {
        return NimbusJwtDecoder.withSecretKey(chaveJwt).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /** Senhas armazenadas apenas como hash BCrypt. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private AuthenticationEntryPoint naoAutenticado() {
        return (requisicao, resposta, excecao) -> escreverProblema(resposta, 401, "Não autenticado",
                "Token ausente, inválido ou expirado.");
    }

    private AccessDeniedHandler acessoNegado() {
        return (requisicao, resposta, excecao) -> escreverProblema(resposta, 403, "Acesso negado",
                "Seu perfil não tem permissão para esta operação.");
    }

    private static void escreverProblema(HttpServletResponse resposta, int status, String titulo, String detalhe)
            throws IOException {
        resposta.setStatus(status);
        resposta.setContentType("application/problem+json");
        resposta.setCharacterEncoding("UTF-8");
        resposta.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + titulo + "\",\"status\":" + status
                + ",\"detail\":\"" + detalhe + "\"}");
    }
}
