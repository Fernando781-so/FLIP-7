package clientejuego;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Cliente {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean continuar = true; 

        do {
            try {
                // Código para conectarse al servidor
                Socket socket = new Socket("localhost", 8080);
                DataOutputStream salida = new DataOutputStream(socket.getOutputStream());
                DataInputStream entrada = new DataInputStream(socket.getInputStream());
                
                System.out.println("\n*** MENU PRINCIPAL ***");
                System.out.println("1. LOGIN");
                System.out.println("2. REGISTRAR");
                System.out.println("3. SALIR");
                System.out.print("Opción: ");
                
                String opcion = scanner.nextLine();
                
                // Salir del programa
                if (opcion.equals("3")) {
                    System.out.println("Cerrando cliente...");
                    socket.close();
                    break; 
                }

                String comando = "LOGIN"; 
                if (opcion.equals("2")) {
                    comando = "REGISTRAR";
                }
                
                // Solicitar usuario y contraseña
                System.out.print("Ingrese Usuario: ");
                String usuario = scanner.nextLine();
                
                System.out.print("Ingrese Contraseña: ");
                String password = scanner.nextLine();
                
                // Enviar credenciales.
                String mensajeProtocolo = comando + ":" + usuario + ":" + password;
                salida.writeUTF(mensajeProtocolo);
                salida.flush();
                
                // Recibir respuesta del servidor.
                String respuesta = entrada.readUTF();
                System.out.println("\n[Servidor]: " + respuesta);
                
                // Cerramos este socket porque el servidor (HiloCliente) cierra tras responder
                socket.close(); 

                // Valida dación para entrar al menú de juego
                if (respuesta.startsWith("Registro exitoso") || respuesta.startsWith("Sesion iniciada")) {
                    
                    boolean enMenuJuego = true; // Control del segundo menú

                    System.out.println("\n¡Bienvenido al sistema de juego, " + usuario + "!");

                    while (enMenuJuego) {
                        System.out.println("\n*** MENU DE JUEGO ***");
                        System.out.println("1. Crear juego");
                        System.out.println("2. Unirse a sala");
                        System.out.println("3. Cerrar sesión");
                        System.out.print("Elige una opción: ");

                        String opcionJuego = scanner.nextLine();

                        switch (opcionJuego) {
                            case "1":
                                System.out.println("Crando sala...");
                                // Aqui deberás implementar la lógica para crear un juego
                                break;

                            case "2":
                                System.out.println("Buscando la sala para UNIRSE...");
                                //Aqui se debe implementar la lógica para unirse a una sala
                                break;

                            case "3":
                                System.out.println("Cerrando sesión...");
                                enMenuJuego = false; // Aqui se rompe el ciclo del menú de juego
                                break;

                            default:
                                System.out.println("Opción invalida.");
                                break;
                        }
                    }
                } 
                // Si la respuesta fue "fallido" o "incorrectas", el if se ignora 
                // y el ciclo principal (do-while) vuelve a empezar pidiendo Login/Registro.

            } catch (IOException e) {
                System.err.println("Error de conexión: " + e.getMessage());
                continuar = false; 
            }
        } while (continuar);
        
        scanner.close(); 
    }
}