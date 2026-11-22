package br.com.raizesdonordeste_api.cliente;

import java.time.LocalDate;
import java.time.Period;
import java.util.EnumMap;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.raizesdonordeste_api.auditoria.AcaoAuditoria;
import br.com.raizesdonordeste_api.auditoria.AuditoriaService;
import br.com.raizesdonordeste_api.comum.ConflitoException;
import br.com.raizesdonordeste_api.comum.RecursoNaoEncontradoException;
import br.com.raizesdonordeste_api.comum.RegraNegocioException;
import br.com.raizesdonordeste_api.seguranca.ControleAcesso;
import br.com.raizesdonordeste_api.seguranca.UsuarioLogado;

/** Cadastro do cliente e direitos do titular previstos na LGPD (RF02, RF19). */
@Service
public class ClienteService {

    /** Versão do termo de consentimento vigente; cada registro guarda a versão aceita. */
    public static final String VERSAO_TERMO = "v1";

    private final ClienteRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoria;

    public ClienteService(ClienteRepository repository, PasswordEncoder passwordEncoder, AuditoriaService auditoria) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.auditoria = auditoria;
    }

    @Transactional
    public ClienteResponse cadastrar(CadastroClienteRequest req) {
        if (req.dataNascimento() != null && Period.between(req.dataNascimento(), LocalDate.now()).getYears() < 18) {
            throw new RegraNegocioException("O programa de fidelidade é restrito a maiores de 18 anos (RN11).");
        }
        String email = req.email().trim().toLowerCase();
        if (repository.existsByEmail(email)) {
            throw new ConflitoException("Já existe cliente cadastrado com este e-mail.");
        }
        Cliente cliente = new Cliente(req.nome().trim(), email, req.telefone(), passwordEncoder.encode(req.senha()),
                req.dataNascimento());
        cliente.registrarConsentimento(Finalidade.FIDELIDADE, req.consentimentoFidelidade(), VERSAO_TERMO);
        cliente.registrarConsentimento(Finalidade.MARKETING, req.consentimentoMarketing(), VERSAO_TERMO);
        return ClienteResponse.de(repository.save(cliente));
    }

    /** Direito de acesso do titular (LGPD, art. 18); o acesso fica registrado na auditoria. */
    @Transactional
    public ClienteResponse meusDados() {
        Cliente cliente = clienteAtual();
        auditoria.registrar(AcaoAuditoria.ACESSO_DADOS_PESSOAIS, null, "Cliente", cliente.getId(), null, null,
                "Acesso do titular aos próprios dados");
        return ClienteResponse.de(cliente);
    }

    @Transactional(readOnly = true)
    public ConsentimentosResponse consentimentos() {
        return montarConsentimentos(clienteAtual());
    }

    /** Concede ou revoga uma finalidade; a revogação tem efeito imediato (RN10) e é auditada. */
    @Transactional
    public ConsentimentosResponse alterarConsentimento(Finalidade finalidade, boolean concedido) {
        Cliente cliente = clienteAtual();
        boolean anterior = cliente.possuiConsentimento(finalidade);
        cliente.registrarConsentimento(finalidade, concedido, VERSAO_TERMO);
        if (anterior && !concedido) {
            auditoria.registrar(AcaoAuditoria.REVOGACAO_CONSENTIMENTO, null, "Cliente", cliente.getId(),
                    finalidade + "=concedido", finalidade + "=revogado", "Revogação pelo titular");
        }
        return montarConsentimentos(cliente);
    }

    /**
     * Eliminação a pedido do titular (RN18): revoga os consentimentos e anonimiza os dados
     * pessoais; os pedidos continuam existindo, sem vínculo com uma pessoa identificável.
     */
    @Transactional
    public void eliminar() {
        Cliente cliente = clienteAtual();
        for (Finalidade finalidade : Finalidade.values()) {
            cliente.registrarConsentimento(finalidade, false, VERSAO_TERMO);
        }
        cliente.anonimizar();
        auditoria.registrar(AcaoAuditoria.ANONIMIZACAO_CLIENTE, null, "Cliente", cliente.getId(), null, null,
                "Eliminação de dados solicitada pelo titular");
    }

    private Cliente clienteAtual() {
        UsuarioLogado usuario = ControleAcesso.exigirAutenticado();
        if (!usuario.isCliente()) {
            throw new AccessDeniedException("Operação exclusiva do cliente titular dos dados.");
        }
        return repository.findById(usuario.id())
                .filter(c -> !c.isAnonimizado())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado."));
    }

    private static ConsentimentosResponse montarConsentimentos(Cliente cliente) {
        Map<Finalidade, Boolean> situacao = new EnumMap<>(Finalidade.class);
        for (Finalidade finalidade : Finalidade.values()) {
            situacao.put(finalidade, cliente.possuiConsentimento(finalidade));
        }
        var historico = cliente.getConsentimentos().stream()
                .map(c -> new ConsentimentosResponse.Registro(c.getFinalidade(), c.isConcedido(), c.getVersaoTermo(),
                        c.getDataHora()))
                .toList();
        return new ConsentimentosResponse(situacao, historico);
    }
}
