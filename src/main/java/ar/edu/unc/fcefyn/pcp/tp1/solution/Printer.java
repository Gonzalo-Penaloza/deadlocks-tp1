package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.PrinterSnapshot;
import ar.edu.unc.fcefyn.pcp.tp1.api.PrinterState;

/**
 * Una impresora de la matriz.
 */
class Printer {

    private final String id;
    private final int row;
    private final int column;

    private PrinterState state = PrinterState.AVAILABLE;
    private int usageCount = 0;
    private Integer assignedOrderId = null;   // null = no tiene orden asignada

    Printer(int row, int column) {
        this.row = row;
        this.column = column;
        this.id = "P-" + row + "-" + column;
    }

    String id() {
        return id;
    }

    int row() {
        return row;
    }

    int column() {
        return column;
    }

    PrinterState state() {
        return state;
    }

    Integer assignedOrderId() {
        return assignedOrderId;
    }

    /** AVAILABLE -> RESERVED. Suma un uso y la asocia a la orden. */
    void reserve(int orderId) {
        requireState(PrinterState.AVAILABLE);
        state = PrinterState.RESERVED;
        usageCount++;
        assignedOrderId = orderId;
    }

    /** RESERVED -> AVAILABLE. */
    void release() {
        requireState(PrinterState.RESERVED);
        state = PrinterState.AVAILABLE;
        assignedOrderId = null;
    }

    /** RESERVED -> OUT_OF_SERVICE. No hay transicion de salida: no se reutiliza. */
    void markOutOfService() {
        requireState(PrinterState.RESERVED);
        state = PrinterState.OUT_OF_SERVICE;
        assignedOrderId = null;
    }

    PrinterSnapshot snapshot() {
        return new PrinterSnapshot(id, state, usageCount, assignedOrderId);
    }

    private void requireState(PrinterState expected) {
        if (state != expected) {
            throw new IllegalStateException("La impresora " + id + " esta en " + state
                    + " y se esperaba " + expected);
        }
    }
}