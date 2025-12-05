package org.yourcompany.yourproject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class HiloCliente extends Thread {
    //Atributos de la clase.
    private final Socket socket;
    private BaseDeDatos db; //Referencia a la base de datos.

    private DataInputStream entrada;
    private DataOutputStream salida;

    //Constructor de la clase.
    public HiloCliente(Socket socket, BaseDeDatos db) {
        this.socket = socket;
        this.db = db;//Inicializamos la referencia a la base de datos.
    }

    //Run del hilo.
    @Override 
    public void  run() {
        try{
            //Inicializamos las variables de entrada y salida.
            entrada = new DataInputStream(socket.getInputStream());
            salida = new DataOutputStream(socket.getOutputStream());
            
            //Leemos el mensaje del cliente.
            String mensaje = entrada.readUTF();
            
            //Dividimos el mensaje en partes usando ":" como separador.
            String[] partes = mensaje.split(":");

            //Condicion para verificar que el formato sea correcto.
            if (partes.length != 3){
                salida.writeUTF("Esta mal Wey. Usa el formato Comando: Usuario, Contraseña.");
            }
            
            // Partimos el mensaje en comando, usuario y contraseña.
            String comando = partes[0];
            String usuario = partes[1];
            String password = partes[2];
            
            // Switch para manejar los comandos REGISTRAR y LOGIN
            switch (comando) {
                case "REGISTRAR":
                    // Si el registro es exitoso
                    if (db.registrarUsuario(usuario, password)) {
                        salida.writeUTF("Registro exitoso.");
                    } else { // Si el usuario ya existe
                        salida.writeUTF("Registro fallido: Usuario ya existe.");
                    }
                    break;
                case "LOGIN":
                    // Si las credenciales son correctas
                    if (db.validarUsuario(usuario, password)) {
                        salida.writeUTF("Sesion iniciada correctamente.");
                    } else { // Si las credenciales son incorrectas
                        salida.writeUTF("La sesion no pudo iniciarse: Usuario/Contraseña incorrectas.");
                    }
                    break;
                default: //Default en caso de que el comando no sea reconocido
                    salida.writeUTF("Comando no reconocido: Por favor use REGISTRAR o LOGIN.");
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } try {//Cerramos el socket.
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}