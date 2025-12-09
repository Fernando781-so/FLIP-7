package org.yourcompany.yourproject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

public class HiloCliente extends Thread {
    
    private final Socket socket;
    private final BaseDeDatos db;
    
    private DataInputStream entrada;
    private DataOutputStream salida;
    private ProcesadorMensajes procesador; 
    
    private String usuarioActual = null; 
    private boolean conectado = true;
    
    // IMPORTANTE: Referencia a la sala actual del jugador
    private Sala salaActual = null; 

    public HiloCliente(Socket socket, BaseDeDatos db) {
        this.socket = socket;
        this.db = db;
    }

    @Override 
    public void run() {
        try {
            entrada = new DataInputStream(socket.getInputStream());
            salida = new DataOutputStream(socket.getOutputStream());

            // Pasamos 'this' al procesador
            this.procesador = new ProcesadorMensajes(this, db);

            while (conectado) {
                try {
                    // 1. Leemos el mensaje
                    String mensajeSucio = entrada.readUTF();
                    
                    // 2. IMPORTANTE: Limpiamos espacios en blanco o saltos de línea
                    String mensaje = mensajeSucio.trim();

                    if (!mensaje.isEmpty()) {
                        System.out.println("DEBUG RECIBIDO [" + usuarioActual + "]: " + mensaje);
                        procesador.procesar(mensaje); 
                    }
                    
                } catch (EOFException e) {
                    cerrarConexion();
                }
            }
        } catch (IOException e) {
            System.out.println("Error conexión: " + e.getMessage());
            cerrarConexion();
        } 
    }

    // Agregamos synchronized para evitar errores si varios hilos envían a la vez
    public synchronized void enviarMensaje(String msg) {
        try {
            if (salida != null) {
                salida.writeUTF(msg);
                salida.flush(); // Asegura que el mensaje salga INMEDIATAMENTE
            }
        } catch (IOException e) {
            System.out.println("No se pudo enviar mensaje a " + usuarioActual);
        }
    }

    public void cerrarConexion() {
        conectado = false;
        // Si está en una sala, lo sacamos
        if (salaActual != null) {
            salaActual.removerJugador(this);
        }
        if (usuarioActual != null) {
            Servidor.clientesOnline.remove(usuarioActual);
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Getters y Setters para la Sala
    public Sala getSalaActual() { return salaActual; }
    public void setSalaActual(Sala sala) { this.salaActual = sala; }

    public String getUsuarioActual() { return usuarioActual; }
    public void setUsuarioActual(String usuarioActual) {
        this.usuarioActual = usuarioActual;
        Servidor.clientesOnline.put(usuarioActual, this);
    }
}