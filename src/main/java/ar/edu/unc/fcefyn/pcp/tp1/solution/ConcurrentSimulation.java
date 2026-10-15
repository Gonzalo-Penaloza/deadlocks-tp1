package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.OrderSnapshot;
import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;
import ar.edu.unc.fcefyn.pcp.tp1.api.PrinterSnapshot;
import ar.edu.unc.fcefyn.pcp.tp1.api.Simulation;
import ar.edu.unc.fcefyn.pcp.tp1.api.SimulationConfig;
import ar.edu.unc.fcefyn.pcp.tp1.api.SimulationResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Arma la granja completa y la pone a funcionar.
 * Los hilos de las cuatro etapas arrancan juntos. Cada etapa termina sola cuando
 * su buffer de entrada queda vacio y la etapa anterior ya termino.
 * El hilo que llama a execute espera a todos con join() y despues arma los resultados.
 */
public final class ConcurrentSimulation implements Simulation {

    @Override
    public SimulationResult execute(SimulationConfig config) throws InterruptedException {
        long start = System.nanoTime();

        SimulationLogger logger = createLogger(config);
        List<Thread> allThreads = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        List<OrderBuffer> buffers;
        OrderRegistry orders;
        PrinterManager printerManager;

        try {
            // 1) Recursos compartidos: uno solo de cada uno para todos los hilos
            orders = new OrderRegistry(config.totalOrders(), logger);
            printerManager = new PrinterManager(config.printerRows(), config.printerColumns());

            // Buffers entre etapas: cada uno sabe cuantos hilos le depositan ordenes
            int capacity = config.totalOrders();
            OrderBuffer bufferCreadas = new OrderBuffer(capacity, 1);
            OrderBuffer bufferAValidacion = new OrderBuffer(capacity, config.assignmentThreads());
            OrderBuffer bufferAImpresion = new OrderBuffer(capacity, config.validationThreads());
            OrderBuffer bufferACalidad = new OrderBuffer(capacity, config.printingThreads());
            buffers = List.of(bufferCreadas, bufferAValidacion, bufferAImpresion, bufferACalidad);

            // 2) Cargar las ordenes nuevas
            for (int id = 1; id <= config.totalOrders(); id++) {
                bufferCreadas.push(id);
            }
            bufferCreadas.markAsFinished();

            // 3) Crear los hilos de las cuatro etapas
            for (int i = 1; i <= config.assignmentThreads(); i++) {
                allThreads.add(newThread("assignment-" + i, errors, new AssignmentWorker(
                        config, bufferCreadas, bufferAValidacion, orders, printerManager)));
            }
            for (int i = 1; i <= config.validationThreads(); i++) {
                allThreads.add(newThread("validation-" + i, errors, new ValidationWorker(
                        config, bufferAValidacion, bufferAImpresion, orders, printerManager)));
            }
            for (int i = 1; i <= config.printingThreads(); i++) {
                allThreads.add(newThread("printing-" + i, errors, new PrintingWorker(
                        config, bufferAImpresion, bufferACalidad, orders, printerManager)));
            }
            for (int i = 1; i <= config.qualityControlThreads(); i++) {
                allThreads.add(newThread("quality-control-" + i, errors, new QualityControlWorker(
                        config, bufferACalidad, orders)));
            }

            // 4) Arrancar todos juntos
            for (Thread t : allThreads) {
                t.start();
            }

            // 5) Esperar a que terminen todos
            for (Thread t : allThreads) {
                t.join();
            }
        } finally {
            logger.close();
        }

        // 6) Armar el resultado (despues del join se ven todos los cambios de los hilos)
        long durationMillis = (System.nanoTime() - start) / 1_000_000;
        boolean allThreadsTerminated = allThreads.stream().noneMatch(Thread::isAlive);

        List<OrderSnapshot> orderSnapshots = orders.snapshot();
        List<PrinterSnapshot> printerSnapshots = printerManager.snapshot();

        Map<OrderState, Integer> totals = new EnumMap<>(OrderState.class);
        for (OrderState state : OrderState.values()) {
            totals.put(state, 0);
        }
        int processed = 0;
        for (OrderSnapshot order : orderSnapshots) {
            totals.merge(order.state(), 1, Integer::sum);
            if (order.state().isTerminal()) {
                processed++;
            }
        }

        SimulationResult result = new SimulationResult(config.totalOrders(), processed, totals,
                orderSnapshots, printerSnapshots, durationMillis, allThreadsTerminated);

        // 7) Escribir elementos.csv y resumen.properties
        try {
            ResultsCollector.write(result, config.outputDirectory());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudieron escribir los resultados", e);
        }

        // Si algun hilo fallo, o quedaron ordenes sin terminar, se informa el error
        synchronized (errors) {
            if (!errors.isEmpty()) {
                IllegalStateException error = new IllegalStateException(
                        errors.size() + " hilo(s) terminaron con error");
                errors.forEach(error::addSuppressed);
                throw error;
            }
        }
        int remainingInBuffers = buffers.stream().mapToInt(OrderBuffer::size).sum();
        if (processed != config.totalOrders() || remainingInBuffers != 0) {
            throw new IllegalStateException("Quedaron ordenes sin terminar: procesadas " + processed
                    + " de " + config.totalOrders() + ", en buffers " + remainingInBuffers);
        }
        return result;
    }

    private static SimulationLogger createLogger(SimulationConfig config) {
        try {
            return new SimulationLogger(config.outputDirectory());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear eventos.csv", e);
        }
    }

    /** Crea un hilo con nombre. Si el worker lanza una excepcion, se guarda para informarla al final. */
    private static Thread newThread(String name, List<Throwable> errors, Runnable worker) {
        Thread thread = new Thread(worker, name);
        thread.setUncaughtExceptionHandler((t, e) -> {
            synchronized (errors) {
                errors.add(e);
            }
        });
        return thread;
    }
}