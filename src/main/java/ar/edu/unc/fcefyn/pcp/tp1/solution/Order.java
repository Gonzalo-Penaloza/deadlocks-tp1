package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.OrderSnapshot;
import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;

/**
 * Una orden de impresion: su estado actual, la impresora que uso y cuantas
 * veces paso por cada etapa.
 * No se sincroniza por si misma: solo la modifica OrderRegistry,
 * que atiende a un hilo por vez.
 */
class Order {

    private final int id;
    private OrderState state = OrderState.CREATED;
    private String printerId;          // se conserva aunque la impresora se libere

    private int assignmentCount;
    private int validationCount;
    private int printingCount;
    private int qualityControlCount;

    Order(int id) {
        this.id = id;
    }

    OrderState state() {
        return state;
    }

    String printerId() {
        return printerId;
    }

    void setPrinterId(String printerId) {
        this.printerId = printerId;
    }

    /** Cambia el estado y suma uno al contador de la etapa. */
    void changeState(String stage, OrderState from, OrderState to) {
        if (state != from) {
            throw new IllegalStateException("La orden " + id + " esta en " + state
                    + " y se intento pasarla de " + from + " a " + to);
        }
        if (!isAllowed(stage, from, to)) {
            throw new IllegalStateException("Transicion no permitida en " + stage + ": " + from + " -> " + to);
        }
        state = to;
        switch (stage) {
            case OrderRegistry.ASSIGNMENT -> assignmentCount++;
            case OrderRegistry.VALIDATION -> validationCount++;
            case OrderRegistry.PRINTING -> printingCount++;
            case OrderRegistry.QUALITY_CONTROL -> qualityControlCount++;
            default -> throw new IllegalArgumentException("Etapa desconocida: " + stage);
        }
    }

    OrderSnapshot snapshot() {
        return new OrderSnapshot(id, state, printerId,
                assignmentCount, validationCount, printingCount, qualityControlCount);
    }

    /** Solo se permiten los caminos del enunciado. */
    private static boolean isAllowed(String stage, OrderState from, OrderState to) {
        return switch (stage) {
            case OrderRegistry.ASSIGNMENT ->
                    from == OrderState.CREATED && to == OrderState.WAITING_VALIDATION;
            case OrderRegistry.VALIDATION -> from == OrderState.WAITING_VALIDATION
                    && (to == OrderState.READY_TO_PRINT || to == OrderState.REJECTED);
            case OrderRegistry.PRINTING -> from == OrderState.READY_TO_PRINT
                    && (to == OrderState.PRINTED || to == OrderState.PRINT_FAILED);
            case OrderRegistry.QUALITY_CONTROL -> from == OrderState.PRINTED
                    && (to == OrderState.APPROVED || to == OrderState.DEFECTIVE);
            default -> false;
        };
    }
}