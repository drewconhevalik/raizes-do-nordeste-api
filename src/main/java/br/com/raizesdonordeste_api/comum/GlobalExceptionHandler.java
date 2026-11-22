package br.com.raizesdonordeste_api.comum;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Converte as exceções da aplicação em respostas HTTP no formato Problem Details (RFC 9457).
 * Erros de validação de entrada (400) são tratados pela classe-mãe.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        return problema;
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail tratar(RecursoNaoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", e.getMessage());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ProblemDetail tratar(RegraNegocioException e) {
        return problema(HttpStatus.valueOf(422), "Regra de negócio violada", e.getMessage());
    }

    @ExceptionHandler(ConflitoException.class)
    public ProblemDetail tratar(ConflitoException e) {
        return problema(HttpStatus.CONFLICT, "Conflito", e.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail tratar(ObjectOptimisticLockingFailureException e) {
        return problema(HttpStatus.CONFLICT, "Conflito de concorrência",
                "O registro foi alterado por outra operação ao mesmo tempo. Tente novamente.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail tratar(DataIntegrityViolationException e) {
        return problema(HttpStatus.CONFLICT, "Violação de integridade",
                "A operação viola uma restrição de integridade dos dados.");
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ProblemDetail tratar(CredenciaisInvalidasException e) {
        return problema(HttpStatus.UNAUTHORIZED, "Não autenticado", e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail tratar(AccessDeniedException e) {
        return problema(HttpStatus.FORBIDDEN, "Acesso negado",
                "Seu perfil não tem permissão para esta operação.");
    }

    @ExceptionHandler(ServicoIndisponivelException.class)
    public ProblemDetail tratar(ServicoIndisponivelException e) {
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "Serviço indisponível", e.getMessage());
    }
}
