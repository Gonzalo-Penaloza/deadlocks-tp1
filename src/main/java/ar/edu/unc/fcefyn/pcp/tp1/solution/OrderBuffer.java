package ar.edu.unc.fcefyn.pcp.tp1.solution;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Buffer sincronizado para pasar órdenes entre etapas (Productor-Consumidor).
 * Cumple con la restricción de NO usar BlockingQueue.
 */
public class OrderBuffer {

    private final Queue<Integer> queue = new LinkedList<>();
    private final int capacity;
    private int activeProducers;

    /**
     * @param capacity  cantidad maxima de ordenes en espera
     * @param producers cantidad de hilos que van a depositar ordenes en este buffer
     */
    public OrderBuffer(int capacity, int producers) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity debe ser mayor que cero");
        }
        if (producers <= 0) {
            throw new IllegalArgumentException("producers debe ser mayor que cero");
        }
        this.capacity = capacity;
        this.activeProducers = producers;
    }

    /** Buffer con un unico productor. */
    public OrderBuffer(int capacity) {
        this(capacity, 1);
    }

    /**
     * Deposita una orden. Si el buffer esta lleno, el productor espera a que
     * un consumidor saque alguna.
     */
    public synchronized void push(int orderId) throws InterruptedException {
        if (activeProducers == 0) {
            // Un push despues del cierre es un error de programacion: lo hacemos
            // visible en lugar de descartar la orden en silencio.
            throw new IllegalStateException("Push de la orden " + orderId + " en un buffer cerrado");
        }
        while (queue.size() == capacity) {
            wait();
        }
        queue.add(orderId);
        notifyAll();   // despierta a los consumidores que esperaban una orden
    }

    /**
     * Retira una orden. Si el buffer esta vacio pero todavia hay productores
     * activos, el consumidor espera.
     *
     * @return la siguiente orden, o {null} si el buffer esta vacio y ya
     *         terminaron todos los productores
     */
    public synchronized Integer pop() throws InterruptedException {
        while (queue.isEmpty() && activeProducers > 0) {
            wait();
        }
        if (queue.isEmpty()) {
            return null;
        }
        Integer order = queue.poll();
        notifyAll();   // despierta a los productores que esperaban lugar
        return order;
    }

    /** Un productor avisa que no va a depositar mas ordenes. */
    public synchronized void markAsFinished() {
        if (activeProducers == 0) {
            throw new IllegalStateException("markAsFinished llamado mas veces que productores");
        }
        activeProducers--;
        if (activeProducers == 0) {
            notifyAll();   // despierta a TODOS los consumidores para que terminen
        }
    }

    /** Cantidad de ordenes en espera (para verificar que no quedan elementos al final). */
    public synchronized int size() {
        return queue.size();
    }
}
