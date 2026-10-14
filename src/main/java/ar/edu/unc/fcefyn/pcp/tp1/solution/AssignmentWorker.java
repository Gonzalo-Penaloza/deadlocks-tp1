package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.SimulationConfig;

/**
 * Etapa 1: Asignacion de impresora.
 * Cada hilo saca una orden nueva de la bandeja de entrada y le pide una
 * impresora libre a la granja. Si estan todas ocupadas, el hilo se queda
 * esperando hasta que alguna etapa libere una.
 * Cuando la consigue, anota el cambio en eventos.csv y deja la orden en
 * la bandeja que lee validacion. Al terminar avisa que no manda mas ordenes.
 */
public class AssignmentWorker implements Runnable {
    private final SimulationConfig config;
    private final OrderBuffer input;
    private final OrderBuffer output;
    private final SimulationLogger logger;
    private final PrinterManager printerManager;

    public AssignmentWorker(SimulationConfig config, OrderBuffer input, OrderBuffer output,
                            SimulationLogger logger, PrinterManager printerManager) {
        this.config = config;
        this.input = input;
        this.output = output;
        this.logger = logger;
        this.printerManager = printerManager;
    }

    /**
     * tomar orden → demora → reservar impresora → anotar → pasar a validación
     */
    @Override
    public void run() {
        try {
            while (true) {
                Integer orderId = input.pop();
                if (orderId == null) {
                    break;
                }
                Thread.sleep(config.assignmentDelayMillis());
                String printerId = printerManager.reservePrinter(orderId);
                logger.logEvent(0, Thread.currentThread().getName(), orderId, "ASSIGNMENT",
                        "ORDER_STATE_CHANGED", "CREATED", "WAITING_VALIDATION", printerId);
                output.push(orderId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            output.markAsFinished();
        }
    }
}