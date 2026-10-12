package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.OutcomeDecider;
import ar.edu.unc.fcefyn.pcp.tp1.api.PrinterState;
import ar.edu.unc.fcefyn.pcp.tp1.api.SimulationConfig;

/**
 * Etapa 3: Impresión.
 * Ejecuta la impresión física en la máquina.
 */
// Inicio Cambios Recientes - Etapa 3 (Impresión)
public class PrintingWorker implements Runnable {
    private final SimulationConfig config;
    private final OrderBuffer input;
    private final OrderBuffer output;
    private final SimulationLogger logger;
    private final PrinterManager printerManager;

    public PrintingWorker(SimulationConfig config, OrderBuffer input, OrderBuffer output, SimulationLogger logger, PrinterManager printerManager) {
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
                    System.out.println("[DEBUG_LOG] [" + Thread.currentThread().getName() + "] Finalizando impresor.");
                    output.markAsFinished();
                    break;
                }

                String printerId = printerManager.getPrinterForOrder(orderId);
                System.out.println("[DEBUG_LOG] [" + Thread.currentThread().getName() + "] Imprimiendo Orden: " + orderId + " (Impresora: " + printerId + ")");
                Thread.sleep(config.printingDelayMillis());
                
                boolean success = OutcomeDecider.isPrintSuccessful(orderId, config);
                
                if (success) {
                    System.out.println("[DEBUG_LOG] [" + Thread.currentThread().getName() + "] Impresion EXITOSA Orden " + orderId + " -> Enviando a Control Calidad.");
                    logger.logEvent(0, Thread.currentThread().getName(), orderId, "PRINTING", "ORDER_STATE_CHANGED", "READY_TO_PRINT", "PRINTED", printerId);
                    printerManager.releasePrinter(printerId);
                    output.push(orderId);
                } else {
                    System.out.println("[DEBUG_LOG] [" + Thread.currentThread().getName() + "] Impresion FALLIDA Orden " + orderId + " -> Impresora FUERA DE SERVICIO.");
                    logger.logEvent(0, Thread.currentThread().getName(), orderId, "PRINTING", "ORDER_STATE_CHANGED", "READY_TO_PRINT", "PRINT_FAILED", printerId);
                    printerManager.updatePrinterState(printerId, PrinterState.OUT_OF_SERVICE);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
// Fin Cambios Recientes
