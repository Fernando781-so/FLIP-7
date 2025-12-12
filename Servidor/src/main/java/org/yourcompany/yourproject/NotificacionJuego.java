package org.yourcompany.yourproject;

public interface NotificacionJuego {
    void enviarMensajePrivado(String nombreUsuario, String mensaje);
    void broadcast(String mensaje);
    void onFinDeJuego(String ganador);
}