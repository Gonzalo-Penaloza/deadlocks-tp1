package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.OrderSnapshot;
import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;
import ar.edu.unc.fcefyn.pcp.tp1.api.SimulationResult;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public final class ResultsCollector {

    private static final String ORDERS_HEADER =
            "orderId;finalState;printer;assignmentCount;validationCount;printingCount;qualityControlCount";

    private ResultsCollector() {
    }

    public static void write(SimulationResult result, Path outputDirectory) throws IOException {
        Files.createDirectories(outputDirectory);
        writeOrders(result, outputDirectory.resolve("elementos.csv"));
        writeSummary(result, outputDirectory.resolve("resumen.properties"));
    }

    private static void writeOrders(SimulationResult result, Path file) throws IOException {
        List<OrderSnapshot> sorted = result.orders().stream()
                .sorted(Comparator.comparingInt(OrderSnapshot::id))
                .toList();

        try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            out.write(ORDERS_HEADER);
            out.write('\n');
            for (OrderSnapshot o : sorted) {
                String printer = o.assignedPrinterId() == null ? "" : o.assignedPrinterId();
                out.write(o.id() + ";" + o.state() + ";" + printer + ";"
                        + o.assignmentCount() + ";" + o.validationCount() + ";"
                        + o.printingCount() + ";" + o.qualityControlCount());
                out.write('\n');
            }
        }
    }

    private static void writeSummary(SimulationResult result, Path file) throws IOException {
        long remaining = result.orders().stream().filter(o -> !o.state().isTerminal()).count();

        try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            out.write("totalOrders=" + result.totalOrders() + "\n");
            out.write("processedOrders=" + result.processedOrders() + "\n");
            out.write("approvedOrders=" + count(result, OrderState.APPROVED) + "\n");
            out.write("rejectedOrders=" + count(result, OrderState.REJECTED) + "\n");
            out.write("printFailedOrders=" + count(result, OrderState.PRINT_FAILED) + "\n");
            out.write("defectiveOrders=" + count(result, OrderState.DEFECTIVE) + "\n");
            out.write("durationMillis=" + result.durationMillis() + "\n");
            out.write("allThreadsTerminated=" + result.allThreadsTerminated() + "\n");
            out.write("remainingIntermediateOrders=" + remaining + "\n");
        }
    }

    private static long count(SimulationResult result, OrderState state) {
        return result.orders().stream().filter(o -> o.state() == state).count();
    }
}
