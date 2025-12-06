package org.yourcompany.yourproject;

import java.io.IOException;
// Importamos Jugador tal como está en tu proyecto original
import org.yourcompany.yourproject.Cartas.src.main.java.Mazo.Jugador; 

public class ProcesadorMensajes {

    private final HiloCliente cliente;
    private final BaseDeDatos db;

    public ProcesadorMensajes(HiloCliente cliente, BaseDeDatos db) {
        this.cliente = cliente;
        this.db = db;
    }

    public void procesar(String mensaje) {
        String[] partes = mensaje.split(":");
        String comando = partes[0];

        switch (comando) {
            case "REGISTRAR":
                handleRegistro(partes);
                break;

            case "LOGIN":
                handleLogin(partes);
                break;
            
            case "CREAR_SALA":
                handleCrearSala();
                break;

            case "UNIRSE_SALA":
                handleUnirseSala();
                break;

            case "SALIR":
                cliente.enviarMensaje("Cerrando conexión. ¡Adiós!");
                cliente.cerrarConexion();
                break;

            default:
                cliente.enviarMensaje("Comando no reconocido.");
                break;
        }
    }

            //metodos de los comandos

    private void handleRegistro(String[] partes) {
        if (partes.length < 3) {
            cliente.enviarMensaje("Error: Faltan datos para registrar.");
            return;
        }

        if (db.registrarUsuario(partes[1], partes[2])) {
            cliente.enviarMensaje("Registro exitoso.");
        } else {
            cliente.enviarMensaje("Registro fallido: Usuario ya existe.");
        }
    }

    private void handleLogin(String[] partes) {
        if (partes.length < 3) {
            cliente.enviarMensaje("Error: Faltan datos para login.");
            return;
        }

        if (db.validarUsuario(partes[1], partes[2])) {
            String usuario = partes[1];
            
            cliente.setUsuarioActual(usuario); 
            
            Jugador nuevoJugador = new Jugador(usuario); //
            cliente.enviarMensaje("Sesion iniciada. bienvenido: " + nuevoJugador.getNombre());
        } else {
            cliente.enviarMensaje("Error: Usuario/Contraseña incorrectas.");
        }
    }

    private void handleCrearSala() {
        if (cliente.getUsuarioActual() == null) {
            cliente.enviarMensaje("Error: Debes iniciar sesión primero.");
        } else {
            System.out.println("Usuario " + cliente.getUsuarioActual() + " quiere crear sala.");
            cliente.enviarMensaje("Sala creada con éxito (ID: Pendiente).");
        }
    }

    private void handleUnirseSala() {
        if (cliente.getUsuarioActual() == null) {
            cliente.enviarMensaje("Error: Debes iniciar sesión primero.");
        } else {
            System.out.println("Usuario " + cliente.getUsuarioActual() + " busca unirse.");
            cliente.enviarMensaje("Unido a la sala (ID: Pendiente).");
        }
    }
}