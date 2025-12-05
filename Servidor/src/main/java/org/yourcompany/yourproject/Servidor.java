package org.yourcompany.yourproject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Servidor {

    public static void main(String[] args) {
        try {
            ServerSocket servidor = new ServerSocket(8080);
            Socket sc;

            System.out.println("El server ha iniciado...");
            
            BaseDeDatos db = new BaseDeDatos();

            while (true) { 
                sc = servidor.accept();
                System.out.println("Un usuario se ha conectado al servidor.");

                HiloCliente hilo = new HiloCliente(sc, db);
                hilo.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
