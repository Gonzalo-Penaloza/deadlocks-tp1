package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;
import ar.edu.unc.fcefyn.pcp.tp1.api.OutcomeDecider;
import ar.edu.unc.fcefyn.pcp.tp1.api.SimulationConfig;

/**
 * Etapa 2: Validación del modelo.
 * Verifica si el diseño es apto para imprimir.
 */
public class ValidationWorker implements Runnable {
    private final SimulationConfig config;
    private final OrderBuffer input;
    private final OrderBuffer output;
    private final OrderRegistry orders;
    private final PrinterManager printerManager;

    public ValidationWorker(SimulationConfig config, OrderBuffer input, OrderBuffer output,
                            OrderRegistry orders, PrinterManager printerManager) {
        this.config = config;
        this.input = input;
        this.output = output;
        this.orders = orders;
        this.printerManager = printerManager;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Integer orderId = input.pop();
                if (orderId == null) {
                    break;
                }

                String printerId = printerManager.getPrinterForOrder(orderId);
                Thread.sleep(config.validationDelayMillis());

                boolean isValid = OutcomeDecider.isModelValid(orderId, config);

                if (isValid) {
                    // La impresora sigue reservada para la impresion.
                    orders.transition(orderId, OrderRegistry.VALIDATION,
                            OrderState.WAITING_VALIDATION, OrderState.READY_TO_PRINT, printerId);
                    output.push(orderId);
                } else {
                    // Primero se anota el rechazo y despues se libera la impresora.
                    orders.transition(orderId, OrderRegistry.VALIDATION,
                            OrderState.WAITING_VALIDATION, OrderState.REJECTED, printerId);
                    printerManager.releasePrinter(printerId);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // Siempre se avisa, aunque haya un error: si no, impresion esperaria para siempre.
            output.markAsFinished();
        }
    }
}