package org.yourcompany.yourproject.Cartas.src.main.java.Mazo;

import org.yourcompany.yourproject.MotorJuego;

public class CartaNumerica extends Carta {

    public CartaNumerica(int valor) {
        super(valor, TipoAccion.NUMERO);
    }

    @Override
    public void activar(MotorJuego motor, Jugador jugador) {
        // Le decimos al motor que procese la lógica de números (explosiones, sumas)
        // NOTA: Necesitarás hacer público el método 'procesarNumero' en MotorJuego
        motor.procesarNumero(jugador, this);
    }
}