package br.com.raizesdonordeste_api.unidade;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.raizesdonordeste_api.comum.RecursoNaoEncontradoException;

@Service
public class UnidadeService {

    private final UnidadeRepository repository;

    public UnidadeService(UnidadeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<UnidadeResponse> listarAtivas() {
        return repository.findByAtivaTrueOrderByNome().stream().map(UnidadeResponse::de).toList();
    }

    @Transactional
    public UnidadeResponse criar(UnidadeRequest req) {
        Unidade unidade = new Unidade(req.nome(), req.cidade(), req.uf(), req.regiao(), req.formato());
        return UnidadeResponse.de(repository.save(unidade));
    }

    @Transactional
    public UnidadeResponse atualizar(Integer id, UnidadeRequest req) {
        Unidade unidade = buscar(id);
        unidade.setNome(req.nome());
        unidade.setCidade(req.cidade());
        unidade.setUf(req.uf());
        unidade.setRegiao(req.regiao());
        unidade.setFormato(req.formato());
        return UnidadeResponse.de(unidade);
    }

    @Transactional(readOnly = true)
    public Unidade buscar(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade " + id + " não encontrada."));
    }
}
