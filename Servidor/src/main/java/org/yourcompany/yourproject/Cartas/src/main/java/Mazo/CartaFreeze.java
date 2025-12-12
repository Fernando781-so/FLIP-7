package org.yourcompany.yourproject.Cartas.src.main.java.Mazo;

import org.yourcompany.yourproject.MotorJuego;

public class CartaFreeze extends Carta {

    public CartaFreeze() {
        super(0, TipoAccion.FREEZE);
    }

    @Override
    public void activar(MotorJuego motor, Jugador jugador) {
        // Configuramos el motor para esperar una selección
        motor.solicitarSeleccion(jugador, TipoAccion.FREEZE, "¿A quién congelas?");
    }
}