package clientejuego;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Cliente {
    
    private static boolean enSala = false;
    private static boolean esAnfitrion = false;
    private static boolean esperandoObjetivo = false; 
    private static boolean juegoIniciado = false;
    private static boolean conectado = true;
    private static boolean enVotacion = false; // Variable auxiliar si la usas

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String host = "localhost";
        int puerto = 8080;

        try {
            System.out.println("Conectando al servidor...");
            Socket socket = new Socket(host, puerto);
            DataOutputStream salida = new DataOutputStream(socket.getOutputStream());
            DataInputStream entrada = new DataInputStream(socket.getInputStream());

            Thread hiloEscucha = new Thread(() -> {
                try {
                    while (conectado) {
                        String msg = entrada.readUTF();
                        procesarMensajeServidor(msg);
                    }
                } catch (IOException e) {
                    System.out.println("\n[Sistema]: Desconectado del servidor.");
                    conectado = false;
                    System.exit(0);
                }
            });
            hiloEscucha.start();


            boolean logueado = false;
            while (!logueado && conectado) {
                System.out.println("\n*** BIENVENIDO ***");
                System.out.println("1. LOGIN");
                System.out.println("2. REGISTRAR");
                System.out.print("Opción: ");
                String opcion = scanner.nextLine();

                if (opcion.equals("1") || opcion.equals("2")) {
                    String comando = opcion.equals("1") ? "LOGIN" : "REGISTRAR";
                    System.out.print("Usuario: ");
                    String user = scanner.nextLine();
                    System.out.print("Contraseña: ");
                    String pass = scanner.nextLine();
                    
                    salida.writeUTF(comando + ":" + user + ":" + pass);
                    
                    try { Thread.sleep(500); } catch (InterruptedException e) {}
                    
                    if (ClientState.ultimoMensajeExito) { 
                        logueado = true;
                    }
                }
            }

            // INICIO DEL BUCLE PRINCIPAL DE ENTRADA CON LÓGICA CONSOLIDADA
            while (conectado) {

                if (esperandoObjetivo) {
                    System.out.print(">> Escribe el nombre del jugador objetivo: ");
                    String objetivo = scanner.nextLine();
                    salida.writeUTF("SELECCIONAR_OBJETIVO:" + objetivo);
                    esperandoObjetivo = false;
                    System.out.println("[Sistema]: Objetivo enviado. Esperando respuesta del servidor...");
                    try { Thread.sleep(500); } catch (InterruptedException e) {}
                }

                else if (juegoIniciado) {
                    // *** RUTA PRINCIPAL JUEGO ***
                    System.out.print(">> Comando: ");
                    String input = scanner.nextLine();
                    salida.writeUTF(input);
                }

                else if (enSala) {
                    System.out.println("\n--- EN SALA DE ESPERA ---");
                    if (esAnfitrion) {
                        // ACTUALIZADO: Se muestra la opción GUARDAR
                        System.out.println("Escribe 'INICIAR_PARTIDA', 'INVITAR:Usuario', 'GUARDAR' o 'SALIR_SALA'.");
                    } else {
                        System.out.println("Esperando a que el líder inicie. Escribe 'SALIR_SALA'.");
                    }
                    System.out.print("Acción: ");
                    String input = scanner.nextLine();
                    salida.writeUTF(input);
                    
                    if (input.equalsIgnoreCase("SALIR_SALA")) {
                        enSala = false;
                        esAnfitrion = false;
                    }
                }

                else { // Menú principal (no está en sala ni en juego)
                    System.out.println("\n*** MENU DE JUEGO ***");
                    System.out.println("1. Crear sala");
                    System.out.println("2. Unirse a sala (UNIRSE:ID)");
                    System.out.println("3. Salir");
                    System.out.println("4. Cargar Partida"); // NUEVA OPCIÓN
                    System.out.print("Acción: ");
                    
                    String input = scanner.nextLine();
                    
                    if (input.equals("1")) {
                        salida.writeUTF("CREAR_SALA");
                    } else if (input.startsWith("UNIRSE:")) { 
                        String[] partes = input.split(":");
                        if(partes.length > 1) salida.writeUTF("UNIRSE_SALA:" + partes[1]);
                    } else if (input.startsWith("ACEPTAR_INVITACION:")) {
                        salida.writeUTF(input);
                    } else if (input.equals("3")) {
                        salida.writeUTF("SALIR");
                        conectado = false;
                    // --- AQUÍ ESTÁ EL ELSE IF QUE NECESITAS ---
                    } else if (input.equals("4")) {
                        System.out.print("Introduce el ID de la partida a cargar: ");
                        String idPartida = scanner.nextLine();
                        salida.writeUTF("CARGAR_PARTIDA:" + idPartida);
                    // ------------------------------------------
                    } else {
                        salida.writeUTF(input);
                    }
                }
            }
            // FIN DEL BUCLE PRINCIPAL DE ENTRADA

            socket.close();
            scanner.close();

        } catch (IOException e) {
            System.out.println("Error: No se pudo conectar con el servidor.");
        }
    }

    private static void procesarMensajeServidor(String msg) {

        System.out.println("\n[Servidor]: " + msg);
        
        if (msg.startsWith("Sesion iniciada")) {
            ClientState.ultimoMensajeExito = true;
        }
        else if (msg.startsWith("Sala creada")) {
            enSala = true;
            esAnfitrion = true;
        }
        else if (msg.startsWith("LOBBY_UPDATE")) {
            enSala = true; 
        }
        else if (msg.startsWith("JUEGO_INICIADO")) {
            juegoIniciado = true;
            System.out.println("!!! LA PARTIDA HA COMENZADO !!!");
        }
        else if (msg.startsWith("SALIDA_EXITOSA")) {
            enSala = false;
            esAnfitrion = false;
            juegoIniciado = false; 
        }
        else if (msg.startsWith("INVITACION")) {
            String[] partes = msg.split(":");
            System.out.println("******************************************");
            System.out.println("* " + partes[1] + " te invita a jugar en Sala " + partes[2]);
            System.out.println("* Escribe: ACEPTAR_INVITACION:" + partes[2]);
            System.out.println("******************************************");
        }
        else if (msg.startsWith("SELECCIONAR")) {
            esperandoObjetivo = true;
            String[] partes = msg.split(":"); 
            System.out.println(">>> ACCIÓN REQUERIDA: " + partes[2]);
            System.out.println(">>> Candidatos: " + (partes.length > 3 ? partes[3] : ""));
        }
        else if (msg.startsWith("SALIDA_EXITOSA") || msg.startsWith("SALIDA_FORZADA")) {
            enSala = false;
            esAnfitrion = false;
            juegoIniciado = false; 
            enVotacion = false;
        }
      
    }
    
    static class ClientState {
        public static volatile boolean ultimoMensajeExito = false;
    }
}