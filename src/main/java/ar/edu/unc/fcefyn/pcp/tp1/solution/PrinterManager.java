package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.PrinterSnapshot;
import ar.edu.unc.fcefyn.pcp.tp1.api.PrinterState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Granja de impresoras: el recurso compartido entre las etapas de asignacion,
 * validacion e impresion.
 *
 * <p>Es un monitor: todos los metodos publicos son {synchronized}, asi que
 * un solo hilo a la vez puede consultar o cambiar el estado de las impresoras.
 * Un hilo que necesita una impresora y no hay ninguna libre espera con
 * {wait()} y se despierta cuando alguien libera o inhabilita una.</p>
 *
 * <p>Se usa {synchronized} + {wait/notifyAll} y no {ReentrantLock}
 * porque con un {Lock} solo se puede esperar usando { Condition}, que
 * el enunciado no permite.</p>
 *
 * <p><b>Orden recomendado respecto del registro de eventos:</b> al asignar, primero
 * { #reservePrinter} y despues registrar el evento; al liberar, primero
 * registrar el evento y despues {#releasePrinter}. Asi el log nunca muestra
 * una impresora asignada a una orden nueva antes de haber sido liberada.</p>
 */
public class PrinterManager {

    private final Printer[][] matrix;
    private final Map<String, Printer> printersById = new HashMap<>();
    private final Map<Integer, Printer> printersByOrder = new HashMap<>();
    private final Deque<Printer> available = new ArrayDeque<>();
    private int operationalPrinters;

    public PrinterManager(int rows, int columns) {
        if (rows <= 0 || columns <= 0) {
            throw new IllegalArgumentException("La matriz debe tener filas y columnas positivas");
        }
        matrix = new Printer[rows][columns];
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                Printer printer = new Printer(row, column);
                matrix[row][column] = printer;
                printersById.put(printer.id(), printer);
                available.addLast(printer);
            }
        }
        operationalPrinters = rows * columns;
    }

    /**
     * Etapa 1: obtiene una impresora disponible, la reserva en exclusiva, suma un
     * uso y la asocia a la orden. Si no hay ninguna disponible, espera.
     *
     * @return el id de la impresora reservada, por ejemplo "P-3-7"
     */
    public synchronized String reservePrinter(int orderId) throws InterruptedException {
        if (printersByOrder.containsKey(orderId)) {
            throw new IllegalStateException("La orden " + orderId + " ya tiene una impresora reservada");
        }
        while (available.isEmpty()) {
            if (operationalPrinters == 0) {
                // No ocurre con las configuraciones de la catedra; evita esperar para siempre.
                throw new IllegalStateException("No quedan impresoras operativas");
            }
            wait();   // suelta el monitor y duerme hasta que se libere o inhabilite una
        }
        Printer printer = available.pollFirst();
        printer.reserve(orderId);
        printersByOrder.put(orderId, printer);
        return printer.id();
    }

    /** Id de la impresora reservada por la orden, o null si no tiene. */
    public synchronized String getPrinterForOrder(int orderId) {
        Printer printer = printersByOrder.get(orderId);
        return printer == null ? null : printer.id();
    }

    /** Etapas 2 (rechazo) y 3 (impresion exitosa): RESERVED -> AVAILABLE. */
    public synchronized void releasePrinter(String printerId) {
        Printer printer = reservedPrinter(printerId);
        printersByOrder.remove(printer.assignedOrderId());
        printer.release();
        available.addLast(printer);
        notifyAll();   // despierta a los asignadores que esperaban una impresora
    }

    /** Etapa 3 (impresion fallida): RESERVED -> OUT_OF_SERVICE. Nunca vuelve a estar disponible. */
    public synchronized void markOutOfService(String printerId) {
        Printer printer = reservedPrinter(printerId);
        printersByOrder.remove(printer.assignedOrderId());
        printer.markOutOfService();
        operationalPrinters--;
        notifyAll();   // si se agotaran las impresoras, los que esperan deben enterarse
    }

    /**
     * Compatibilidad con el codigo de las etapas 2 y 3: AVAILABLE equivale a
     * {@link #releasePrinter} y OUT_OF_SERVICE a {@link #markOutOfService}.
     */
    public synchronized void updatePrinterState(String printerId, PrinterState newState) {
        switch (newState) {
            case AVAILABLE -> releasePrinter(printerId);
            case OUT_OF_SERVICE -> markOutOfService(printerId);
            case RESERVED -> throw new IllegalArgumentException("Para reservar se usa reservePrinter");
        }
    }

    /** Estado de todas las impresoras, ordenadas por fila y columna. */
    public synchronized List<PrinterSnapshot> snapshot() {
        List<PrinterSnapshot> result = new ArrayList<>();
        for (Printer[] row : matrix) {
            for (Printer printer : row) {
                result.add(printer.snapshot());
            }
        }
        return result;
    }

    // Se invoca con el monitor tomado.
    private Printer reservedPrinter(String printerId) {
        Printer printer = printersById.get(printerId);
        if (printer == null) {
            throw new IllegalArgumentException("No existe la impresora " + printerId);
        }
        if (printer.state() != PrinterState.RESERVED) {
            throw new IllegalStateException("La impresora " + printerId + " no esta reservada");
        }
        return printer;
    }
}