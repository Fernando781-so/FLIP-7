package org.yourcompany.yourproject.Cartas.src.main.java.Mazo;

public class Carta {
    private final int valor;
    private final TipoAccion tipo;

    public Carta(int valor) {
        if (valor < 0 || valor > 12) {
            throw new IllegalArgumentException("El valor debe estar entre 0 y 12.");
        }
        this.valor = valor;
        this.tipo = TipoAccion.NUMERO;
    }

    
    public Carta(TipoAccion tipo) {
        this.valor = 0; 
        this.tipo = tipo;
    }

    public int getValor() { return valor; }
    public TipoAccion getTipo() { return tipo; }
    
}