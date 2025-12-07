package clientejuego;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException; 
import java.net.Socket;
import java.util.Scanner;

public class Cliente {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean continuar = true; 
        String Host = "localhost";
        do {
            try {
                System.out.println("\n*** MENU PRINCIPAL ***");
                System.out.println("1. LOGIN");
                System.out.println("2. REGISTRAR");
                System.out.println("3. SALIR");
                System.out.print("Opción: ");

                String opcion = scanner.nextLine();

                if (opcion.equals("3")) {
                    System.out.println("Cerrando cliente...");
                    continuar = false; 
                    break; 
                }
                
                if (!opcion.equals("1") && !opcion.equals("2")) {
                    System.out.println("Opción invalida. Intente de nuevo.");
                    continue;
                }

                Socket socket = new Socket( Host, 8080);
                
                DataOutputStream salida = new DataOutputStream(socket.getOutputStream());
                DataInputStream entrada = new DataInputStream(socket.getInputStream());

                String comando = "LOGIN"; 
                if (opcion.equals("2")) {
                    comando = "REGISTRAR";
                }

                System.out.print("Ingrese Usuario: ");
                String usuario = scanner.nextLine();

                System.out.print("Ingrese Contraseña: ");
                String password = scanner.nextLine();

                String mensajeProtocolo = comando + ":" + usuario + ":" + password;
                salida.writeUTF(mensajeProtocolo);
                salida.flush();

                String respuesta = entrada.readUTF();
                System.out.println("\n[Servidor]: " + respuesta);

                socket.close(); 

                if (respuesta.startsWith("Registro exitoso") || respuesta.startsWith("Sesion iniciada")) {

                    boolean enMenuJuego = true; 

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

            } catch (ConnectException e) {
                System.out.println("\n Error de Conexión: No se pudo conectar al servidor en" + Host + ". Verifique que el servidor esté en ejecución.");
             

            } catch (IOException e) {
                System.out.println("\n Error de Comunicación/IO: " );
     
                
            } catch (Exception e) {
 
                 System.out.println("\n Error inesperado: ");
            }
        } while (continuar);

        scanner.close(); 
    }
}