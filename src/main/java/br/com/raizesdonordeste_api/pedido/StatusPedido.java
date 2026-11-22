package br.com.raizesdonordeste_api.pedido;

/** Ciclo de vida do pedido (RN05). */
public enum StatusPedido {
    AGUARDANDO_PAGAMENTO, PAGO, EM_PREPARO, PRONTO, ENTREGUE, CANCELADO;

    /** Indica se a transição para o novo status é permitida pelo ciclo de vida. */
    public boolean podeIrPara(StatusPedido novo) {
        return switch (this) {
            case AGUARDANDO_PAGAMENTO -> novo == PAGO || novo == CANCELADO;
            case PAGO -> novo == EM_PREPARO || novo == CANCELADO;
            case EM_PREPARO -> novo == PRONTO || novo == CANCELADO;
            case PRONTO -> novo == ENTREGUE;
            case ENTREGUE, CANCELADO -> false;
        };
    }
}
