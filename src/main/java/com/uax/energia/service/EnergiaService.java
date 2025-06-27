package com.uax.energia.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class EnergiaService {
    private final int HOGARES = 5;
    private final int ESTACIONES = 2;
    private final List<FuenteRenovable> renovables = new CopyOnWriteArrayList<>();

    private final Semaphore energiaDisponible = new Semaphore(50, true); // Capacidad inicial
    private final AtomicInteger energiaTotal = new AtomicInteger(50);
    private volatile boolean enMarcha = false;

    private final List<Hogar> hogares = new ArrayList<>();
    private final List<Estacion> estaciones = new ArrayList<>();

    @Async
    public CompletableFuture<Void> iniciarSimulacion() {
        if (enMarcha) return CompletableFuture.completedFuture(null);
        enMarcha = true;
        hogares.clear();
        estaciones.clear();
        for (int i = 0; i < ESTACIONES; i++) {
            Estacion estacion = new Estacion("E" + (i+1), energiaDisponible, energiaTotal, renovables);
            estaciones.add(estacion);
            new Thread(estacion).start();
        }
        for (int i = 0; i < HOGARES; i++) {
            Hogar hogar = new Hogar("H" + (i+1), energiaDisponible);
            hogares.add(hogar);
            new Thread(hogar).start();
        }
        return CompletableFuture.completedFuture(null);
    }

    public void anadirRenovable(String tipo, int capacidad) {
        renovables.add(new FuenteRenovable(tipo, capacidad));
        energiaDisponible.release(capacidad);
        energiaTotal.addAndGet(capacidad);
    }

    public Map<String, Object> estadoActual() {
        Map<String, Object> estado = new LinkedHashMap<>();
        estado.put("energia_total", energiaTotal.get());
        estado.put("energia_disponible", energiaDisponible.availablePermits());
        estado.put("hogares", hogares.stream().map(h -> Map.of(
            "id", h.id,
            "consumo", h.ultimoConsumo
        )).toList());
        estado.put("renovables", renovables.stream().map(r -> Map.of(
            "tipo", r.tipo,
            "capacidad", r.capacidad
        )).toList());
        return estado;
    }

    // Clases internas

    static class Hogar implements Runnable {
        String id;
        Semaphore energiaDisponible;
        int ultimoConsumo = 0;
        Random random = new Random();

        Hogar(String id, Semaphore energiaDisponible) {
            this.id = id;
            this.energiaDisponible = energiaDisponible;
        }

        public void run() {
            while (true) {
                try {
                    int consumo = 2 + random.nextInt(4); // entre 2 y 5 unidades
                    energiaDisponible.acquire(consumo);
                    ultimoConsumo = consumo;
                    Thread.sleep(400 + random.nextInt(200));
                    energiaDisponible.release(consumo);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    static class Estacion implements Runnable {
        String id;
        Semaphore energiaDisponible;
        AtomicInteger energiaTotal;
        List<FuenteRenovable> renovables;
        Random random = new Random();

        Estacion(String id, Semaphore energiaDisponible, AtomicInteger energiaTotal, List<FuenteRenovable> renovables) {
            this.id = id;
            this.energiaDisponible = energiaDisponible;
            this.energiaTotal = energiaTotal;
            this.renovables = renovables;
        }

        public void run() {
            while (true) {
                try {
                    int produccion = 5 + random.nextInt(5); // entre 5 y 9 unidades
                    energiaDisponible.release(produccion);
                    energiaTotal.addAndGet(produccion);
                    Thread.sleep(1000 + random.nextInt(500));
                } catch (Exception ignored) {}
            }
        }
    }

    static class FuenteRenovable {
        String tipo;
        int capacidad;
        FuenteRenovable(String tipo, int capacidad) {
            this.tipo = tipo;
            this.capacidad = capacidad;
        }
    }
}