package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultsCollectorTest {

    @TempDir
    Path dir;

    @Test
    void writesOrdersSortedAndSummaryWithCounts() throws Exception {
        List<OrderSnapshot> orders = List.of(
                new OrderSnapshot(2, OrderState.REJECTED, "P-0-1", 1, 1, 0, 0),
                new OrderSnapshot(1, OrderState.APPROVED, "P-0-0", 1, 1, 1, 1),
                new OrderSnapshot(3, OrderState.PRINT_FAILED, "P-0-2", 1, 1, 1, 0),
                new OrderSnapshot(4, OrderState.DEFECTIVE, "P-0-3", 1, 1, 1, 1));
        List<PrinterSnapshot> printers =
                List.of(new PrinterSnapshot("P-0-0", PrinterState.AVAILABLE, 1, null));
        Map<OrderState, Integer> totals = Map.of(
                OrderState.APPROVED, 1, OrderState.REJECTED, 1,
                OrderState.PRINT_FAILED, 1, OrderState.DEFECTIVE, 1);
        SimulationResult result = new SimulationResult(4, 4, totals, orders, printers, 1234L, true);

        ResultsCollector.write(result, dir);

        List<String> lines = Files.readAllLines(dir.resolve("elementos.csv"));
        assertEquals(5, lines.size());
        assertEquals("orderId;finalState;printer;assignmentCount;validationCount;printingCount;qualityControlCount",
                lines.get(0));
        assertEquals("1;APPROVED;P-0-0;1;1;1;1", lines.get(1));   // ordenado por id
        assertEquals("2;REJECTED;P-0-1;1;1;0;0", lines.get(2));

        Properties p = new Properties();
        try (var reader = Files.newBufferedReader(dir.resolve("resumen.properties"))) {
            p.load(reader);
        }
        assertEquals("4", p.getProperty("totalOrders"));
        assertEquals("1", p.getProperty("approvedOrders"));
        assertEquals("1", p.getProperty("rejectedOrders"));
        assertEquals("1", p.getProperty("printFailedOrders"));
        assertEquals("1", p.getProperty("defectiveOrders"));
        assertEquals("1234", p.getProperty("durationMillis"));
        assertEquals("true", p.getProperty("allThreadsTerminated"));
        assertEquals("0", p.getProperty("remainingIntermediateOrders"));
    }

    @Test
    void emptyPrinterFieldAndIntermediateOrdersAreReported() throws Exception {
        List<OrderSnapshot> orders = List.of(
                new OrderSnapshot(1, OrderState.CREATED, null, 0, 0, 0, 0));
        SimulationResult result = new SimulationResult(1, 0, Map.of(OrderState.CREATED, 1),
                orders, List.of(), 5L, false);

        ResultsCollector.write(result, dir);

        assertEquals("1;CREATED;;0;0;0;0", Files.readAllLines(dir.resolve("elementos.csv")).get(1));
        Properties p = new Properties();
        try (var reader = Files.newBufferedReader(dir.resolve("resumen.properties"))) {
            p.load(reader);
        }
        assertEquals("1", p.getProperty("remainingIntermediateOrders"));
        assertEquals("false", p.getProperty("allThreadsTerminated"));
    }
}