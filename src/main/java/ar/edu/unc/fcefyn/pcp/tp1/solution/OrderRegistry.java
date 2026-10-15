package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.OrderSnapshot;
import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;

import java.util.ArrayList;
import java.util.List;

/**
 * Registro de todas las ordenes: guarda el estado de cada una y anota cada
 * cambio en eventos.csv.
 */
public class OrderRegistry {

    // Nombres de las etapas, tal cual los pide eventos.csv
    public static final String ASSIGNMENT = "ASSIGNMENT";
    public static final String VALIDATION = "VALIDATION";
    public static final String PRINTING = "PRINTING";
    public static final String QUALITY_CONTROL = "QUALITY_CONTROL";

    private final Order[] orders;      // la orden N esta en orders[N - 1]
    private final SimulationLogger logger;

    /** Crea las ordenes 1..totalOrders en CREATED y anota ORDER_CREATED de cada una. */
    public OrderRegistry(int totalOrders, SimulationLogger logger) {
        this.logger = logger;
        this.orders = new Order[totalOrders];
        for (int id = 1; id <= totalOrders; id++) {
            orders[id - 1] = new Order(id);
            logger.logOrderCreated(id);
        }
    }

    /**
     * Cambia el estado de una orden y lo anota en eventos.csv.
     * printerId solo se usa en la asignacion; en las demas etapas se anota
     * la impresora que la orden ya tenia guardada.
     */
    public synchronized void transition(int orderId, String stage, OrderState from, OrderState to,
                                        String printerId) {
        Order order = order(orderId);
        order.changeState(stage, from, to);
        if (ASSIGNMENT.equals(stage)){
            order.setPrinterId(printerId);
        }
        logger.logTransition(orderId, stage, from, to, order.printerId());
    }

    /**
     * Foto de todas las ordenes. Se usa al final, despues del join.
     */
    public synchronized List<OrderSnapshot> snapshot() {
        List<OrderSnapshot> result = new ArrayList<>(orders.length);
        for (Order order : orders) {
            result.add(order.snapshot());
        }
        return result;
    }

    private Order order(int orderId) {
        if (orderId < 1 || orderId > orders.length) {
            throw new IllegalArgumentException("No existe la orden " + orderId);
        }
        return orders[orderId - 1];
    }
}