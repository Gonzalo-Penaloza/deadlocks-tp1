package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.PrinterSnapshot;
import ar.edu.unc.fcefyn.pcp.tp1.api.PrinterState;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrinterManagerTest {

    @Test
    void createsTheMatrixWithAllPrintersAvailable() {
        PrinterManager manager = new PrinterManager(2, 3);
        List<PrinterSnapshot> printers = manager.snapshot();
        assertEquals(6, printers.size());
        assertEquals("P-0-0", printers.get(0).id());
        assertEquals("P-1-2", printers.get(5).id());
        for (PrinterSnapshot p : printers) {
            assertEquals(PrinterState.AVAILABLE, p.state());
            assertEquals(0, p.usageCount());
            assertNull(p.assignedOrderId());
        }
    }

    /**
     * Muchos hilos reservan y liberan a la vez sobre pocas impresoras: una impresora
     * nunca puede estar asignada a dos ordenes al mismo tiempo, y el total de usos
     * debe coincidir con la cantidad de reservas.
     */
    @RepeatedTest(30)
    void aPrinterIsNeverAssignedToTwoOrdersAtTheSameTime() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            PrinterManager manager = new PrinterManager(1, 3);   // solo 3 impresoras
            Set<String> inUse = new HashSet<>();                 // impresoras tomadas segun el test
            int[] violations = {0};
            int threads = 6;
            int ordersPerThread = 200;
            List<Thread> workers = new ArrayList<>();

            for (int t = 0; t < threads; t++) {
                int base = t * ordersPerThread;
                workers.add(new Thread(() -> {
                    try {
                        for (int i = 1; i <= ordersPerThread; i++) {
                            String printer = manager.reservePrinter(base + i);
                            synchronized (inUse) {
                                if (!inUse.add(printer)) {
                                    violations[0]++;     // otra orden ya la tenia
                                }
                            }
                            synchronized (inUse) {
                                inUse.remove(printer);
                            }
                            manager.releasePrinter(printer);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }));
            }
            workers.forEach(Thread::start);
            for (Thread w : workers) {
                w.join();
            }

            assertEquals(0, violations[0], "una impresora se asigno a dos ordenes a la vez");
            int totalUses = manager.snapshot().stream().mapToInt(PrinterSnapshot::usageCount).sum();
            assertEquals(threads * ordersPerThread, totalUses);
            for (PrinterSnapshot p : manager.snapshot()) {
                assertEquals(PrinterState.AVAILABLE, p.state());
            }
        });
    }

    /** Sin impresoras libres, el asignador espera hasta que se libere una. */
    @Test
    void reservationWaitsUntilAPrinterIsReleased() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            PrinterManager manager = new PrinterManager(1, 1);
            String first = manager.reservePrinter(1);
            String[] second = new String[1];

            Thread waiting = new Thread(() -> {
                try {
                    second[0] = manager.reservePrinter(2);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            waiting.start();
            Thread.sleep(100);
            assertTrue(waiting.isAlive(), "deberia estar esperando una impresora");

            manager.releasePrinter(first);
            waiting.join();
            assertEquals(first, second[0]);
            assertEquals(first, manager.getPrinterForOrder(2));
            assertNull(manager.getPrinterForOrder(1));
        });
    }

    /** Una impresora fuera de servicio no se vuelve a entregar. */
    @Test
    void outOfServicePrintersAreNeverReused() throws Exception {
        PrinterManager manager = new PrinterManager(1, 2);
        String broken = manager.reservePrinter(1);
        manager.markOutOfService(broken);

        for (int order = 2; order <= 20; order++) {
            String printer = manager.reservePrinter(order);
            assertFalse(printer.equals(broken), "se reutilizo una impresora fuera de servicio");
            manager.releasePrinter(printer);
        }
        PrinterSnapshot brokenSnapshot = manager.snapshot().stream()
                .filter(p -> p.id().equals(broken)).findFirst().orElseThrow();
        assertEquals(PrinterState.OUT_OF_SERVICE, brokenSnapshot.state());
        assertEquals(1, brokenSnapshot.usageCount());
        assertNull(brokenSnapshot.assignedOrderId());
    }

    /** El metodo que usan las etapas 2 y 3 se comporta igual que los nuevos. */
    @Test
    void updatePrinterStateKeepsCompatibility() throws Exception {
        PrinterManager manager = new PrinterManager(1, 2);
        String a = manager.reservePrinter(1);
        String b = manager.reservePrinter(2);
        manager.updatePrinterState(a, PrinterState.AVAILABLE);
        manager.updatePrinterState(b, PrinterState.OUT_OF_SERVICE);

        List<PrinterSnapshot> printers = manager.snapshot();
        assertEquals(PrinterState.AVAILABLE, printers.get(0).state());
        assertEquals(PrinterState.OUT_OF_SERVICE, printers.get(1).state());
    }
}