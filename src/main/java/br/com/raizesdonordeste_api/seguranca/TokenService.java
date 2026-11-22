package br.com.raizesdonordeste_api.seguranca;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Emite tokens JWT assinados (HS256) com o perfil e a unidade de quem fez login. */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final long validadeMinutos;

    public TokenService(JwtEncoder encoder, @Value("${app.jwt.validade-minutos}") long validadeMinutos) {
        this.encoder = encoder;
        this.validadeMinutos = validadeMinutos;
    }

    public String gerar(String assunto, Integer id, String tipo, String papel, Integer unidadeId) {
        Instant agora = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("raizes-do-nordeste-api")
                .issuedAt(agora)
                .expiresAt(agora.plus(validadeMinutos, ChronoUnit.MINUTES))
                .subject(assunto)
                .claim("uid", id)
                .claim("tipo", tipo)
                .claim("roles", List.of(papel));
        if (unidadeId != null) {
            claims.claim("unidadeId", unidadeId);
        }
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(cabecalho, claims.build())).getTokenValue();
    }

    public long validadeSegundos() {
        return validadeMinutos * 60;
    }
}
