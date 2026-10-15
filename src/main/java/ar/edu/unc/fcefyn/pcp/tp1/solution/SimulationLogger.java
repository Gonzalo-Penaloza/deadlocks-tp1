package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Escribe eventos.csv de forma segura entre hilos. Cada fila se escribe
 * en el mismo orden en que se invoca el logger.
 */
public class SimulationLogger {

    public static final String HEADER =
            "sequence;elapsedMs;thread;orderId;stage;event;fromState;toState;printer";

    private final BufferedWriter writer;
    private final long startNanos = System.nanoTime();
    private long sequence = 1;
    private boolean closed = false;

    public SimulationLogger(Path outputDirectory) throws IOException {
        Files.createDirectories(outputDirectory);
        this.writer = Files.newBufferedWriter(
                outputDirectory.resolve("eventos.csv"), StandardCharsets.UTF_8);
        writer.write(HEADER);
        writer.write('\n');
        writer.flush();
    }

    public synchronized void logOrderCreated(int orderId) {
        append(elapsedMillis(), Thread.currentThread().getName(), orderId,
                "INITIALIZATION", "ORDER_CREATED", "", "CREATED", "");
    }

    public synchronized void logTransition(int orderId, String stage,
                                           OrderState from, OrderState to, String printer) {
        append(elapsedMillis(), Thread.currentThread().getName(), orderId,
                stage, "ORDER_STATE_CHANGED", from.name(), to.name(), printer);
    }

    public synchronized void logEvent(long elapsedMs, String thread, int orderId, String stage,
                                      String event, String from, String to, String printer) {
        append(elapsedMs, thread, orderId, stage, event, from, to, printer);
    }

    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private long elapsedMillis() {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private void append(long elapsedMs, String thread, int orderId, String stage,
                        String event, String from, String to, String printer) {
        try {
            writer.write(sequence++ + ";" + Math.max(0, elapsedMs) + ";" + thread + ";" + orderId
                    + ";" + stage + ";" + event + ";" + orEmpty(from) + ";" + orEmpty(to)
                    + ";" + orEmpty(printer));
            writer.write('\n');
            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }
}