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

    public HiloCliente(Socket socket, BaseDeDatos db) {
        this.socket = socket;
        this.db = db;
    }

    @Override 
    public void run() {
        try {
            entrada = new DataInputStream(socket.getInputStream());
            salida = new DataOutputStream(socket.getOutputStream());


            this.procesador = new ProcesadorMensajes(this, db);

            while (conectado) {
                try {
                    String mensaje = entrada.readUTF();
                    

                    procesador.procesar(mensaje); 
                    
                } catch (EOFException e) {
                    cerrarConexion();
                }
            }
        } catch (IOException e) {
            System.out.println("Error conexión: " + e.getMessage());
            cerrarConexion();
        } 
    }

    public void enviarMensaje(String msg) {
        try {
            if (salida != null) {
                salida.writeUTF(msg);
                salida.flush();
            }
        } catch (IOException e) {
            System.out.println("No se pudo enviar mensaje a " + usuarioActual);
        }
    }

    public void cerrarConexion() {
    conectado = false;
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

    public String getUsuarioActual() {
        return usuarioActual;
    }

public void setUsuarioActual(String usuarioActual) {
    this.usuarioActual = usuarioActual;
    Servidor.clientesOnline.put(usuarioActual, this);
}
}