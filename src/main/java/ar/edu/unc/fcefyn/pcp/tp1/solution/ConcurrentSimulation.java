package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.Simulation;
import ar.edu.unc.fcefyn.pcp.tp1.api.SimulationConfig;
import ar.edu.unc.fcefyn.pcp.tp1.api.SimulationResult;
import java.util.ArrayList;
import java.util.List;

public final class ConcurrentSimulation implements Simulation {

    @Override
    public SimulationResult execute(SimulationConfig config) throws InterruptedException {
        try {
            SimulationLogger logger = new SimulationLogger(config.outputDirectory());
            
            // Buffers para conectar las etapas
            OrderBuffer bufferAsignacionAValidacion = new OrderBuffer(100);
            OrderBuffer bufferValidacionAImpresion = new OrderBuffer(100);
            OrderBuffer bufferImpresionACalidad = new OrderBuffer(100);

            // Lista para hacer join de todos los hilos al final
            List<Thread> allThreads = new ArrayList<>();

            // TODO: Integrante 2 - Crear y lanzar hilos de Asignación
            // TODO: Integrante 3 - Crear y lanzar hilos de Validación e Impresión
            // TODO: Integrante 4 - Crear y lanzar hilos de Calidad

            // Esperar a que todos terminen
            for (Thread t : allThreads) {
                t.join();
            }

            logger.close();
            
            // Devolver resultado (debe completarse con datos reales)
            return null; 
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

