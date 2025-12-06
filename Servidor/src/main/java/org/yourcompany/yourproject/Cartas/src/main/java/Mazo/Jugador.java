package org.yourcompany.yourproject.Cartas.src.main.java.Mazo;

import java.util.ArrayList;
import java.util.List;


public class Jugador {
    private String nombre;        // Nombre del usuario [cite: 3]
    private List<Carta> mano;     // Las cartas que tiene el jugador [cite: 9]
    private int puntaje;          // Puntaje dentro del juego [cite: 8]
    private boolean enLinea;      // Estado: en línea o desconectado [cite: 6]
    private boolean jugando;      // EstadoJuego: jugando u ocupado [cite: 7]

    public Jugador(String nombre) {
        this.nombre = nombre;
        this.mano = new ArrayList<>();
        this.puntaje = 0;
        this.enLinea = true;      // Al crear el objeto, asumimos que se conectó
        this.jugando = false;
    }

    // Método para recibir una carta (del Mazo)
    public void recibirCarta(Carta carta) {
        this.mano.add(carta);
        // Aquí podrías actualizar el puntaje automáticamente si lo deseas
    }

    public String getNombre() {
        return nombre;
    }

    public List<Carta> getMano() {
        return mano;
    }

    public int getPuntaje() {
        return puntaje;
    }

    public void setPuntaje(int puntaje) {
        this.puntaje = puntaje;
    }
    
    // Método para calcular puntos basados en la mano actual [cite: 10]
    public void calcularPuntaje() {
        int suma = 0;
        for (Carta c : this.mano) {
            suma += c.getValor();
        }
        this.puntaje = suma;
    }

    @Override
    public String toString() {
        return "Jugador: " + nombre + " | Puntos: " + puntaje + " | Cartas: " + mano.size();
    }
}