package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;
import ar.edu.unc.fcefyn.pcp.tp1.api.OutcomeDecider;
import ar.edu.unc.fcefyn.pcp.tp1.api.SimulationConfig;

/**
 * Etapa 3: Impresión.
 * Ejecuta la impresión física en la máquina.
 */
public class PrintingWorker implements Runnable {
    private final SimulationConfig config;
    private final OrderBuffer input;
    private final OrderBuffer output;
    private final OrderRegistry orders;
    private final PrinterManager printerManager;

    public PrintingWorker(SimulationConfig config, OrderBuffer input, OrderBuffer output,
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
                Thread.sleep(config.printingDelayMillis());

                boolean success = OutcomeDecider.isPrintSuccessful(orderId, config);

                if (success) {
                    // Primero se anota, despues se libera la impresora y se pasa a calidad.
                    orders.transition(orderId, OrderRegistry.PRINTING,
                            OrderState.READY_TO_PRINT, OrderState.PRINTED, printerId);
                    printerManager.releasePrinter(printerId);
                    output.push(orderId);
                } else {
                    // La impresora queda fuera de servicio y no se vuelve a usar.
                    orders.transition(orderId, OrderRegistry.PRINTING,
                            OrderState.READY_TO_PRINT, OrderState.PRINT_FAILED, printerId);
                    printerManager.markOutOfService(printerId);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // Siempre se avisa, aunque haya un error: si no, calidad esperaria para siempre.
            output.markAsFinished();
        }
    }
}