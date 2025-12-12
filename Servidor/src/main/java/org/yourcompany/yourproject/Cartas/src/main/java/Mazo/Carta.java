package org.yourcompany.yourproject.Cartas.src.main.java.Mazo;

import org.yourcompany.yourproject.MotorJuego;


public abstract class Carta {
    protected int valor; 
    protected TipoAccion tipo;

    public Carta(int valor, TipoAccion tipo) {
        this.valor = valor;
        this.tipo = tipo;
    }

    public int getValor() { return valor; }
    public TipoAccion getTipo() { return tipo; }


    public abstract void activar(MotorJuego motor, Jugador jugador);

    @Override
    public String toString() {
        return (tipo == TipoAccion.NUMERO) ? "[" + valor + "]" : "[" + tipo + "]";
    }
}