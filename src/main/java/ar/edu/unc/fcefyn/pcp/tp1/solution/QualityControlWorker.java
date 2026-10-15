package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;
import ar.edu.unc.fcefyn.pcp.tp1.api.OutcomeDecider;
import ar.edu.unc.fcefyn.pcp.tp1.api.SimulationConfig;
/**
 * Etapa 4: Control de calidad.
 * Cada hilo toma una orden impresa, espera la demora de calidad y la pasa
 * a APPROVED o DEFECTIVE segun OutcomeDecider. Es la ultima etapa:
 * no usa impresoras ni tiene buffer de salida.
 */
public class QualityControlWorker implements Runnable {
    private final SimulationConfig config;
    private final OrderBuffer input;
    private final OrderRegistry orders;

    public QualityControlWorker(SimulationConfig config, OrderBuffer input, OrderRegistry orders) {
        this.config = config;
        this.input = input;
        this.orders = orders;

    }

    @Override
    public void run() {
        try {
            while (true) {
                Integer orderId = input.pop();
                if (orderId == null) {
                    break;
                }
                Thread.sleep(config.qualityControlDelayMillis());
                OrderState result;
                if (OutcomeDecider.isQualityApproved(orderId, config)) {
                    result = OrderState.APPROVED;
                } else {
                    result = OrderState.DEFECTIVE;
                }
                orders.transition(orderId, OrderRegistry.QUALITY_CONTROL,OrderState.PRINTED, result, null);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}