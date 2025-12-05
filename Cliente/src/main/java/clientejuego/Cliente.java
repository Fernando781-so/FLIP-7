package clientejuego;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Cliente {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 8080);
            DataOutputStream salida = new DataOutputStream(socket.getOutputStream());
            
            Scanner scanner = new Scanner(System.in);       
            System.out.println("Escribe un mensaje para el servidor: ");
            String mensaje = scanner.nextLine();
            
            salida.writeUTF(mensaje);
            salida.flush();
            scanner.close();
        } catch (IOException e) {
            System.err.println("No se pudo conectar al servidor: " + e.getMessage());
        }
    }
}
