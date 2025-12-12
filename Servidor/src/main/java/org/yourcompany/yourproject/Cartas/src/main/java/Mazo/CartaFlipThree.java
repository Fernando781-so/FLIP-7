package org.yourcompany.yourproject.Cartas.src.main.java.Mazo;

import org.yourcompany.yourproject.MotorJuego;

public class CartaFlipThree extends Carta {

    public CartaFlipThree() {
        super(0, TipoAccion.FLIP_THREE);
    }

    @Override
    public void activar(MotorJuego motor, Jugador jugador) {
        motor.solicitarSeleccion(jugador, TipoAccion.FLIP_THREE, "¿A quién atacas con Flip 3?");
    }
}
