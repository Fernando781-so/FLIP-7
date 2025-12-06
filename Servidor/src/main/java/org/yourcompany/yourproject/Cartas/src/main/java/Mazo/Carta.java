package org.yourcompany.yourproject.Cartas.src.main.java.Mazo;

public class Carta {
    private final int valor;

    public Carta(int valor) {
        if (valor < 0 || valor > 12) {
            throw new IllegalArgumentException("El valor de la carta debe estar entre 1 y 12.");
        }
        this.valor = valor;
    }

    public int getValor() {
        return valor;
    }

    @Override
    public String toString() {
        return "[" + valor + "]";
    }
}