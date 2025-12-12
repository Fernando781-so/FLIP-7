package org.yourcompany.yourproject.Cartas.src.main.java.Mazo;

import org.yourcompany.yourproject.MotorJuego;

public class CartaBono extends Carta {

    public CartaBono(TipoAccion tipo, int valor) {
        super(valor, tipo);
    }

    @Override
    public void activar(MotorJuego motor, Jugador jugador) {
        // Lógica simple: mensaje y pasar turno
        motor.notificarTodos("BONUS: " + jugador.getNombre() + " obtuvo " + this.tipo);
        
        // Si no estamos en medio de un ataque, avanzamos
        if (!motor.isEnModoFlipThree()) {
            motor.avanzarTurno();
        }
    }
}