package org.yourcompany.yourproject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class HiloCliente extends Thread {
    //Atributos de la clase.
    private final Socket socket;
    private DataInputStream entrada;
    private DataOutputStream salida;

    //Constructor de la clase.
    public HiloCliente(Socket socket) {
        this.socket = socket;
    }

    @Override 
    public void  run() {
        try{
            entrada = new DataInputStream(socket.getInputStream());
            salida = new DataOutputStream(socket.getOutputStream());
            
            String mensaje = entrada.readUTF();
            System.out.println("Mensaje recibido del cliente: " + mensaje);
            
            salida.writeUTF("Mensaje recibido: " + mensaje);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}