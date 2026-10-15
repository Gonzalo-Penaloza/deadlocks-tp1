package ar.edu.unc.fcefyn.pcp.tp1.solution;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderBufferTest {

    /**
     * Varios productores y varios consumidores: ninguna orden se pierde ni se
     * entrega dos veces, y todos los hilos terminan. Se repite porque los
     * errores de concurrencia no aparecen en todas las ejecuciones.
     */
    @RepeatedTest(50)
    void noOrderIsLostOrDuplicatedWithSeveralProducers() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            int producers = 3;
            int consumers = 2;
            int perProducer = 500;
            OrderBuffer buffer = new OrderBuffer(10, producers);   // capacidad chica: fuerza esperas
            int[] received = new int[producers * perProducer + 1];
            List<Thread> threads = new ArrayList<>();

            for (int p = 0; p < producers; p++) {
                int first = p * perProducer + 1;
                threads.add(new Thread(() -> {
                    try {
                        for (int id = first; id < first + perProducer; id++) {
                            buffer.push(id);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        buffer.markAsFinished();
                    }
                }));
            }
            for (int c = 0; c < consumers; c++) {
                threads.add(new Thread(() -> {
                    try {
                        Integer id;
                        while ((id = buffer.pop()) != null) {
                            synchronized (received) {
                                received[id]++;
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }));
            }

            threads.forEach(Thread::start);
            for (Thread t : threads) {
                t.join();
            }

            for (int id = 1; id < received.length; id++) {
                assertEquals(1, received[id], "la orden " + id + " se recibio " + received[id] + " veces");
            }
            assertEquals(0, buffer.size());
        });
    }

    /** Un consumidor no debe terminar mientras quede algun productor activo. */
    @Test
    void consumerWaitsUntilTheLastProducerFinishes() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            OrderBuffer buffer = new OrderBuffer(10, 2);
            Thread consumer = new Thread(() -> {
                try {
                    assertNull(buffer.pop());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            consumer.start();

            buffer.markAsFinished();            // termina el primer productor
            Thread.sleep(100);
            assertTrue(consumer.isAlive(), "el consumidor termino con un productor activo");

            buffer.markAsFinished();            // termina el ultimo: el consumidor se despierta
            consumer.join();
        });
    }
}
