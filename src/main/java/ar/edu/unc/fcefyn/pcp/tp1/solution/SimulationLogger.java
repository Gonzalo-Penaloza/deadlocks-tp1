package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.SimulationConfig;
import java.io.PrintWriter;
import java.nio.file.Path;

/**
 * Encargado de escribir los archivos CSV y properties de forma segura entre hilos.
 */
public class SimulationLogger {
    private final PrintWriter eventosWriter;
    private long sequence = 1;

    public SimulationLogger(Path outputDirectory) throws Exception {
        this.eventosWriter = new PrintWriter(outputDirectory.resolve("eventos.csv").toFile());
        this.eventosWriter.println("sequence;elapsedMs;thread;orderId;stage;event;fromState;toState;printer");
    }

    public synchronized void logEvent(long elapsedMs, String thread, int orderId, String stage, String event, String from, String to, String printer) {
        eventosWriter.printf("%d;%d;%s;%d;%s;%s;%s;%s;%s%n",
                sequence++, elapsedMs, thread, orderId, stage, event, from, to, printer);
        eventosWriter.flush();
    }

    public void close() {
        eventosWriter.close();
    }
}
