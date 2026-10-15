package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.PrinterSnapshot;
import ar.edu.unc.fcefyn.pcp.tp1.api.PrinterState;
import ar.edu.unc.fcefyn.pcp.tp1.api.SimulationConfig;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssignmentWorkerTest {

    @TempDir
    Path outputDirectory;

    /**
     * Tres asignadores reparten 60 ordenes entre solo 4 impresoras. Un "validador
     * falso" libera cada impresora despues de recibir la orden, asi los asignadores
     * tienen que esperar y reutilizar impresoras.
     */
    @RepeatedTest(20)
    void threeAssignersProcessEveryOrderExactlyOnce() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            int totalOrders = 60;
            int assigners = 3;
            SimulationConfig config = new SimulationConfig(totalOrders, 2, 2, assigners, 1, 1, 1,
                    100, 100, 100, 0, 0, 0, 0, 1L, outputDirectory);

            OrderBuffer created = new OrderBuffer(totalOrders, 1);
            for (int id = 1; id <= totalOrders; id++) {
                created.push(id);
            }
            created.markAsFinished();

            OrderBuffer waitingValidation = new OrderBuffer(10, assigners);
            PrinterManager printers = new PrinterManager(config.printerRows(), config.printerColumns());
            SimulationLogger logger = new SimulationLogger(outputDirectory);
            OrderRegistry orders = new OrderRegistry(totalOrders, logger);

            List<Thread> threads = new ArrayList<>();
            for (int i = 1; i <= assigners; i++) {
                threads.add(new Thread(new AssignmentWorker(config, created, waitingValidation, orders, printers),
                        "assignment-" + i));
            }

            Set<Integer> received = new HashSet<>();
            int[] duplicates = {0};
            Thread fakeValidator = new Thread(() -> {
                try {
                    Integer id;
                    while ((id = waitingValidation.pop()) != null) {
                        if (!received.add(id)) {
                            duplicates[0]++;
                        }
                        printers.releasePrinter(printers.getPrinterForOrder(id));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "fake-validator");

            threads.forEach(Thread::start);
            fakeValidator.start();
            for (Thread t : threads) {
                t.join();
            }
            fakeValidator.join();   // termina solo si los 3 asignadores llamaron markAsFinished
            logger.close();

            assertEquals(0, duplicates[0], "una orden llego dos veces a validacion");
            assertEquals(totalOrders, received.size(), "se perdieron ordenes");

            int uses = printers.snapshot().stream().mapToInt(PrinterSnapshot::usageCount).sum();
            assertEquals(totalOrders, uses, "los usos no coinciden con las asignaciones");
            for (PrinterSnapshot p : printers.snapshot()) {
                assertEquals(PrinterState.AVAILABLE, p.state());
            }

            List<String> lines = Files.readAllLines(outputDirectory.resolve("eventos.csv"));
            // cabecera + un ORDER_CREATED por orden + una asignacion por orden
            assertEquals(2 * totalOrders + 1, lines.size(), "faltan o sobran eventos");
            for (String line : lines.subList(totalOrders + 1, lines.size())) {
                String[] f = line.split(";", -1);
                assertTrue(f[2].startsWith("assignment-"), line);
                assertEquals("ASSIGNMENT", f[4]);
                assertEquals("CREATED", f[6]);
                assertEquals("WAITING_VALIDATION", f[7]);
                assertTrue(f[8].startsWith("P-"), line);
            }
        });
    }
}