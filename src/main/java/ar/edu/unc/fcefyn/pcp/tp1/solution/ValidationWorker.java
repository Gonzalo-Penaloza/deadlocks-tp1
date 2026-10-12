package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.OutcomeDecider;
import ar.edu.unc.fcefyn.pcp.tp1.api.SimulationConfig;

/**
 * Etapa 2: Validación del modelo.
 * Verifica si el diseño es apto para imprimir.
 */
// Inicio Cambios Recientes - Etapa 2 (Validación)
public class ValidationWorker implements Runnable {
    private final SimulationConfig config;
    private final OrderBuffer input;
    private final OrderBuffer output;
    private final SimulationLogger logger;
    private final PrinterManager printerManager;

    public ValidationWorker(SimulationConfig config, OrderBuffer input, OrderBuffer output, SimulationLogger logger, PrinterManager printerManager) {
        this.config = config;
        this.input = input;
        this.output = output;
        this.logger = logger;
        this.printerManager = printerManager;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Integer orderId = input.pop();
                if (orderId == null) {
                    System.out.println("[DEBUG_LOG] [" + Thread.currentThread().getName() + "] Finalizando validador.");
                    output.markAsFinished();
                    break;
                }

                String printerId = printerManager.getPrinterForOrder(orderId);
                System.out.println("[DEBUG_LOG] [" + Thread.currentThread().getName() + "] Validando Orden: " + orderId + " (Impresora: " + printerId + ")");
                Thread.sleep(config.validationDelayMillis());
                
                boolean isValid = OutcomeDecider.isModelValid(orderId, config);
                
                if (isValid) {
                    System.out.println("[DEBUG_LOG] [" + Thread.currentThread().getName() + "] Orden " + orderId + " VALIDA -> Enviando a Impresion.");
                    logger.logEvent(0, Thread.currentThread().getName(), orderId, "VALIDATION", "ORDER_STATE_CHANGED", "WAITING_VALIDATION", "READY_TO_PRINT", printerId);
                    output.push(orderId);
                } else {
                    System.out.println("[DEBUG_LOG] [" + Thread.currentThread().getName() + "] Orden " + orderId + " RECHAZADA -> Liberando impresora.");
                    logger.logEvent(0, Thread.currentThread().getName(), orderId, "VALIDATION", "ORDER_STATE_CHANGED", "WAITING_VALIDATION", "REJECTED", printerId);
                    printerManager.releasePrinter(printerId);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
// Fin Cambios Recientes
