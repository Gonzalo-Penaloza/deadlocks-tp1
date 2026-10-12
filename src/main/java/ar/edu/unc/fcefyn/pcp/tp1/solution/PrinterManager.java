package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.PrinterState;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.HashMap;
import java.util.Map;

/**
 * Clase temporal para unificar el funcionamiento de las etapas 2 y 3.
 * Reemplazar luego por el codigo correspondiente.-
 */
public class PrinterManager {
    private final Map<Integer, String> orderToPrinter = new HashMap<>();
    private final Map<String, PrinterState> printerStates = new HashMap<>();
    private final Lock lock = new ReentrantLock();

    public void updatePrinterState(String printerId, PrinterState newState) {
        lock.lock();
        try {
            printerStates.put(printerId, newState);
        } finally {
            lock.unlock();
        }
    }

    public void assignOrderToPrinter(int orderId, String printerId) {
        lock.lock();
        try {
            orderToPrinter.put(orderId, printerId);
            printerStates.put(printerId, PrinterState.RESERVED);
        } finally {
            lock.unlock();
        }
    }

    public String getPrinterForOrder(int orderId) {
        lock.lock();
        try {
            return orderToPrinter.get(orderId);
        } finally {
            lock.unlock();
        }
    }

    public void releasePrinter(String printerId) {
        lock.lock();
        try {
            printerStates.put(printerId, PrinterState.AVAILABLE);
        } finally {
            lock.unlock();
        }
    }
}
