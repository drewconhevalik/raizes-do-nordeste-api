package br.com.raizesdonordeste_api.config;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.raizesdonordeste_api.catalogo.CategoriaProduto;
import br.com.raizesdonordeste_api.catalogo.ItemCardapio;
import br.com.raizesdonordeste_api.catalogo.ItemCardapioRepository;
import br.com.raizesdonordeste_api.catalogo.Produto;
import br.com.raizesdonordeste_api.catalogo.ProdutoRepository;
import br.com.raizesdonordeste_api.cliente.Cliente;
import br.com.raizesdonordeste_api.cliente.ClienteRepository;
import br.com.raizesdonordeste_api.cliente.Finalidade;
import br.com.raizesdonordeste_api.estoque.Estoque;
import br.com.raizesdonordeste_api.estoque.EstoqueRepository;
import br.com.raizesdonordeste_api.fidelidade.MovimentoPontos;
import br.com.raizesdonordeste_api.fidelidade.MovimentoPontosRepository;
import br.com.raizesdonordeste_api.fidelidade.TipoMovimentoPontos;
import br.com.raizesdonordeste_api.unidade.FormatoUnidade;
import br.com.raizesdonordeste_api.unidade.Regiao;
import br.com.raizesdonordeste_api.unidade.Unidade;
import br.com.raizesdonordeste_api.unidade.UnidadeRepository;
import br.com.raizesdonordeste_api.usuario.Perfil;
import br.com.raizesdonordeste_api.usuario.Usuario;
import br.com.raizesdonordeste_api.usuario.UsuarioRepository;

/**
 * Dados de demonstração carregados na inicialização (banco H2 em memória).
 * Todas as contas de demonstração usam a senha "Senha@123" (ver README).
 *
 * Os produtos "Água de coco" (R$ 6,97), "Cocada" (R$ 4,98) e "Queijadinha" (R$ 3,99) têm
 * preços escolhidos para acionar os cenários de recusa, erro e demora do serviço de
 * pagamento simulado, que decide o resultado pelos centavos do valor (docs/03-arquitetura.md).
 */
@Component
public class DadosIniciais implements CommandLineRunner {

    public static final String SENHA_DEMO = "Senha@123";

    private final UnidadeRepository unidades;
    private final ProdutoRepository produtos;
    private final ItemCardapioRepository cardapio;
    private final EstoqueRepository estoques;
    private final UsuarioRepository usuarios;
    private final ClienteRepository clientes;
    private final MovimentoPontosRepository movimentosPontos;
    private final PasswordEncoder passwordEncoder;

    public DadosIniciais(UnidadeRepository unidades, ProdutoRepository produtos, ItemCardapioRepository cardapio,
            EstoqueRepository estoques, UsuarioRepository usuarios, ClienteRepository clientes,
            MovimentoPontosRepository movimentosPontos, PasswordEncoder passwordEncoder) {
        this.unidades = unidades;
        this.produtos = produtos;
        this.cardapio = cardapio;
        this.estoques = estoques;
        this.usuarios = usuarios;
        this.clientes = clientes;
        this.movimentosPontos = movimentosPontos;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (unidades.count() > 0) {
            return;
        }

        // Unidades: formatos e regiões diferentes
        Unidade recife = unidades.save(new Unidade("Raízes Boa Viagem", "Recife", "PE", Regiao.NORDESTE, FormatoUnidade.COMPLETA));
        Unidade saoPaulo = unidades.save(new Unidade("Raízes Paulista", "São Paulo", "SP", Regiao.SUDESTE, FormatoUnidade.REDUZIDA));
        Unidade fortaleza = unidades.save(new Unidade("Raízes Iracema", "Fortaleza", "CE", Regiao.NORDESTE, FormatoUnidade.COMPLETA));

        // Catálogo global da matriz
        Produto tapiocaCarne = produto("Tapioca de carne de sol", "Tapioca com carne de sol desfiada e queijo coalho", CategoriaProduto.TAPIOCA, "18.90", false);
        Produto tapiocaQueijo = produto("Tapioca de queijo coalho", "Tapioca com queijo coalho e manteiga de garrafa", CategoriaProduto.TAPIOCA, "14.50", false);
        Produto cuscuz = produto("Cuscuz recheado com charque", "Cuscuz de milho recheado com charque acebolado", CategoriaProduto.CUSCUZ, "22.00", true);
        Produto bolo = produto("Bolo de macaxeira", "Fatia de bolo de macaxeira", CategoriaProduto.BOLO, "9.00", false);
        Produto suco = produto("Suco de graviola", "Suco natural de graviola 400 ml", CategoriaProduto.BEBIDA, "8.50", false);
        Produto cafe = produto("Café coado", "Café passado na hora 200 ml", CategoriaProduto.BEBIDA, "5.00", false);
        Produto cafeCompleto = produto("Café da manhã completo", "Cuscuz, ovo, queijo coalho, tapioca e café", CategoriaProduto.CAFE_DA_MANHA, "32.00", true);
        Produto canjica = produto("Canjica", "Canjica de milho (período junino)", CategoriaProduto.BOLO, "10.00", false);
        Produto aguaCoco = produto("Água de coco", "Água de coco 300 ml", CategoriaProduto.BEBIDA, "6.97", false);
        Produto cocada = produto("Cocada", "Cocada de forno", CategoriaProduto.BOLO, "4.98", false);
        Produto queijadinha = produto("Queijadinha", "Queijadinha de coco", CategoriaProduto.BOLO, "3.99", false);

        // Cardápio e estoque de Recife (cozinha completa): todos os produtos
        for (Produto p : new Produto[] {tapiocaCarne, tapiocaQueijo, cuscuz, suco, cafe, cafeCompleto, aguaCoco, cocada, queijadinha}) {
            ofertar(recife, p, null, null, 50);
        }
        ofertar(recife, bolo, null, null, 2); // estoque baixo para demonstrar "estoque insuficiente"
        ItemCardapio canjicaRecife = ofertar(recife, canjica, null, null, 30);
        int ano = LocalDate.now().getYear();
        canjicaRecife.setVigenciaInicio(LocalDate.of(ano, 6, 1)); // produto sazonal: só no período junino
        canjicaRecife.setVigenciaFim(LocalDate.of(ano, 7, 31));

        // São Paulo (formato reduzido): cuscuz e café completo exigem cozinha completa e não aparecem
        for (Produto p : new Produto[] {tapiocaQueijo, cuscuz, bolo, suco, cafe, cafeCompleto}) {
            ofertar(saoPaulo, p, null, null, 40);
        }
        ofertar(saoPaulo, tapiocaCarne, new BigDecimal("21.90"), null, 40); // preço local diferente

        // Fortaleza: variação regional de receita
        ofertar(fortaleza, tapiocaCarne, null, "Servida com manteiga da terra", 40);
        ofertar(fortaleza, cuscuz, null, null, 40);
        ofertar(fortaleza, suco, null, null, 40);

        // Usuários internos
        String senha = passwordEncoder.encode(SENHA_DEMO);
        usuarios.save(new Usuario(null, "Administrador da Matriz", "admin@rnordeste.com", senha, Perfil.ADMIN_MATRIZ));
        usuarios.save(new Usuario(recife, "Gerente Recife", "gerente.recife@rnordeste.com", senha, Perfil.GERENTE));
        usuarios.save(new Usuario(recife, "Atendente Recife", "atendente.recife@rnordeste.com", senha, Perfil.ATENDENTE));
        usuarios.save(new Usuario(recife, "Cozinha Recife", "cozinha.recife@rnordeste.com", senha, Perfil.COZINHA));
        usuarios.save(new Usuario(saoPaulo, "Gerente São Paulo", "gerente.sp@rnordeste.com", senha, Perfil.GERENTE));

        // Cliente com consentimento de fidelidade e sem consentimento de marketing
        Cliente maria = new Cliente("Maria da Silva", "maria@email.com", "81999990000", senha, LocalDate.of(1990, 5, 10));
        maria.registrarConsentimento(Finalidade.FIDELIDADE, true, "v1");
        maria.registrarConsentimento(Finalidade.MARKETING, false, "v1");
        maria.creditarPontos(250); // saldo inicial de demonstração para o resgate de pontos
        clientes.save(maria);
        movimentosPontos.save(new MovimentoPontos(maria, null, TipoMovimentoPontos.CREDITO, 250));
    }

    private Produto produto(String nome, String descricao, CategoriaProduto categoria, String preco, boolean exigeCozinha) {
        return produtos.save(new Produto(nome, descricao, categoria, new BigDecimal(preco), exigeCozinha));
    }

    private ItemCardapio ofertar(Unidade unidade, Produto produto, BigDecimal precoLocal, String observacao, int quantidade) {
        ItemCardapio item = new ItemCardapio(unidade, produto);
        item.setPrecoLocal(precoLocal);
        item.setObservacaoRegional(observacao);
        estoques.save(new Estoque(unidade, produto, quantidade));
        return cardapio.save(item);
    }
}
