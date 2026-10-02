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
    private boolean finished = false;

    public OrderBuffer(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void push(int orderId) throws InterruptedException {
        while (queue.size() == capacity && !finished) {
            wait();
        }
        if (!finished) {
            queue.add(orderId);
            notifyAll();
        }
    }

    public synchronized Integer pop() throws InterruptedException {
        while (queue.isEmpty() && !finished) {
            wait();
        }
        if (queue.isEmpty() && finished) {
            return null;
        }
        Integer order = queue.poll();
        notifyAll();
        return order;
    }

    public synchronized void markAsFinished() {
        finished = true;
        notifyAll();
    }
}
