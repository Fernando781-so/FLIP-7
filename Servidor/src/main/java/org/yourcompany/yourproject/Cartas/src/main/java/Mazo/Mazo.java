package org.yourcompany.yourproject.Cartas.src.main.java.Mazo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Mazo {
    private final List<Carta> cartas;

    public Mazo() {
        this.cartas = new ArrayList<>();

        // --- 1. Cartas Numéricas ---
        // Agregar el 0 (solo hay uno)
        this.cartas.add(new CartaNumerica(0));

        // Agregar números del 1 al 12
        // La regla suele ser: N copias del número N (ej. cinco 5s, doce 12s)
        for (int valor = 1; valor <= 12; valor++) {
            for (int i = 0; i < valor; i++) {
                this.cartas.add(new CartaNumerica(valor));
            }
        }
        agregarCartasFreeze(3);
        agregarCartasFlipThree(3);
        agregarCartasBono(TipoAccion.SECOND_CHANCE, 0, 3);
        agregarCartasBono(TipoAccion.PUNTOS, 2, 1);
        agregarCartasBono(TipoAccion.PUNTOS, 4, 1);
        agregarCartasBono(TipoAccion.PUNTOS, 6, 1);
        agregarCartasBono(TipoAccion.PUNTOS, 8, 1);
        agregarCartasBono(TipoAccion.PUNTOS, 10, 1);
        
        agregarCartasBono(TipoAccion.MULTIPLICADOR, 2, 1); 
    }

    // --- Métodos Auxiliares de Construcción ---

    private void agregarCartasFreeze(int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            this.cartas.add(new CartaFreeze());
        }
    }

    private void agregarCartasFlipThree(int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            this.cartas.add(new CartaFlipThree());
        }
    }

    private void agregarCartasBono(TipoAccion tipo, int valor, int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            this.cartas.add(new CartaBono(tipo, valor));
        }
    }

    // --- Métodos de Gestión del Mazo ---

    public void barajar() {
        Collections.shuffle(this.cartas);
    }
    
    public Carta tomarCarta() {
        if (cartas.isEmpty()) {
            return null; // O lanzar excepción, según prefieras manejarlo
        }
        return cartas.remove(0);
    }

    public int getTamano() {
        return this.cartas.size();
    }
}