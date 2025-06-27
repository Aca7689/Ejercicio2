package com.uax.energia.controller;

import com.uax.energia.service.EnergiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/energia")
public class EnergiaController {
    @Autowired
    private EnergiaService energiaService;

    @PostMapping("/simular")
    public CompletableFuture<Void> iniciarSimulacion() {
        return energiaService.iniciarSimulacion();
    }

    @GetMapping("/estado")
    public Map<String, Object> estado() {
        return energiaService.estadoActual();
    }

    @PostMapping("/renovable")
    public void anadirRenovable(@RequestParam String tipo, @RequestParam int capacidad) {
        energiaService.anadirRenovable(tipo, capacidad);
    }
}