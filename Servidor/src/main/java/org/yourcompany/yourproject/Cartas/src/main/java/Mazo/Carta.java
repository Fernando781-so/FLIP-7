package org.yourcompany.yourproject.Cartas.src.main.java.Mazo;

public class Carta {
    private final int valor;
    private final TipoAccion tipo;

    public int getValor() { return valor; }
    public TipoAccion getTipo() { return tipo; }

    public Carta(int valor) {
        if (valor < 0 || valor > 12) {
            throw new IllegalArgumentException("El valor debe estar entre 0 y 12.");
        }
        this.valor = valor;
        this.tipo = TipoAccion.NUMERO;
    }
    public Carta(TipoAccion tipo, int valorEspecial) {
        this.tipo = tipo;
        this.valor = valorEspecial; 
    }
    
   
    public Carta(TipoAccion tipo) {
        this(tipo, 0);
    }

    @Override
    public String toString() {
        if (this.tipo == TipoAccion.NUMERO) return "[" + this.valor + "]";
        if (this.tipo == TipoAccion.PUNTOS) return "[+" + this.valor + "]";
        if (this.tipo == TipoAccion.MULTIPLICADOR) return "[x" + this.valor + "]";
        return "[" + this.tipo.toString() + "]";
    }
}