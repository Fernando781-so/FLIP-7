package org.yourcompany.yourproject.Cartas.src.main.java.Mazo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Mazo {
    private final List<Carta> cartas;

    public Mazo() {
        this.cartas = new ArrayList<>();
        this.cartas.add(new Carta(0)); //

        
        for (int valor = 1; valor <= 12; valor++) {
            for (int i = 0; i < valor; i++) {
                this.cartas.add(new Carta(valor)); //
            }
        }

        
        agregarCartasEspeciales(TipoAccion.FREEZE, 3);
        agregarCartasEspeciales(TipoAccion.FLIP_THREE, 3);
        agregarCartasEspeciales(TipoAccion.SECOND_CHANCE, 3);
        agregarCartasConValor(TipoAccion.PUNTOS, 2, 1);  
        agregarCartasConValor(TipoAccion.PUNTOS, 4, 1);  
        agregarCartasConValor(TipoAccion.PUNTOS, 6, 1);  
        agregarCartasConValor(TipoAccion.PUNTOS, 8, 1);  
        agregarCartasConValor(TipoAccion.PUNTOS, 10, 1); 
        agregarCartasConValor(TipoAccion.MULTIPLICADOR, 2, 1);
    }

    private void agregarCartasEspeciales(TipoAccion tipo, int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            this.cartas.add(new Carta(tipo));
        }
    }

    public void barajar() {
        Collections.shuffle(this.cartas);
    }
    
    public Carta tomarCarta() {
        if (cartas.isEmpty()) return null;
        return cartas.remove(0);
    }

    public int getTamano() {
        return this.cartas.size();
    }
    private void agregarCartasConValor(TipoAccion tipo, int valor, int cantidad) {
    for (int i = 0; i < cantidad; i++) {
        this.cartas.add(new Carta(tipo, valor));
    }
}
}