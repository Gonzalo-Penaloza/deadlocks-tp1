package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationLoggerTest {

    @TempDir
    Path dir;

    @Test
    void writesHeaderCreatedRowAndTransitionsWithEmptyFields() throws Exception {
        Path out = dir.resolve("salida");   // carpeta que todavía no existe
        SimulationLogger logger = new SimulationLogger(out);
        logger.logOrderCreated(1);
        logger.logTransition(1, "ASSIGNMENT", OrderState.CREATED, OrderState.WAITING_VALIDATION, "P-0-0");
        logger.logTransition(2, "VALIDATION", OrderState.WAITING_VALIDATION, OrderState.REJECTED, null);
        logger.close();
        logger.close();   // cerrar dos veces no debe fallar

        List<String> lines = Files.readAllLines(out.resolve("eventos.csv"));
        assertEquals(SimulationLogger.HEADER, lines.get(0));
        assertEquals(4, lines.size());

        String[] created = lines.get(1).split(";", -1);
        assertEquals(9, created.length);
        assertEquals("1", created[0]);
        assertEquals("1", created[3]);
        assertEquals("INITIALIZATION", created[4]);
        assertEquals("ORDER_CREATED", created[5]);
        assertEquals("", created[6]);
        assertEquals("CREATED", created[7]);
        assertEquals("", created[8]);

        String[] moved = lines.get(2).split(";", -1);
        assertEquals("2", moved[0]);
        assertEquals("ORDER_STATE_CHANGED", moved[5]);
        assertEquals("CREATED", moved[6]);
        assertEquals("WAITING_VALIDATION", moved[7]);
        assertEquals("P-0-0", moved[8]);

        String[] noPrinter = lines.get(3).split(";", -1);
        assertEquals("", noPrinter[8]);                       // null -> campo vacío
        assertTrue(Long.parseLong(noPrinter[1]) >= 0);        // elapsedMs no negativo
    }

    @Test
    void concurrentWritersKeepSequenceConsecutiveInFileOrder() throws Exception {
        SimulationLogger logger = new SimulationLogger(dir);
        int threads = 4;
        int perThread = 250;
        List<Thread> workers = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            int base = t * perThread;
            Thread worker = new Thread(() -> {
                for (int i = 1; i <= perThread; i++) {
                    logger.logTransition(base + i, "ASSIGNMENT",
                            OrderState.CREATED, OrderState.WAITING_VALIDATION, "P-0-0");
                }
            }, "assignment-" + t);
            worker.start();
            workers.add(worker);
        }
        for (Thread worker : workers) {
            worker.join();
        }
        logger.close();

        List<String> lines = Files.readAllLines(dir.resolve("eventos.csv"));
        assertEquals(threads * perThread + 1, lines.size());
        long expected = 1;
        for (String line : lines.subList(1, lines.size())) {
            String[] fields = line.split(";", -1);
            assertEquals(9, fields.length);
            assertEquals(expected++, Long.parseLong(fields[0]));
        }
    }
}
