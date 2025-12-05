package org.yourcompany.yourproject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BaseDeDatos {
    
    private Connection conexion; 

    // Constructor para conectar a la base de datos SQLite
    public BaseDeDatos() {
        try {
            // Esto creará el archivo 'usuarios.db' en la carpeta de tu proyecto
            String url = "jdbc:sqlite:usuarios.db"; 
            this.conexion = DriverManager.getConnection(url);
            
            crearTabla();
            
        } catch (SQLException e) {
            System.out.println("Error DB: " + e.getMessage());
        }
    }

    private void crearTabla() {
        /*
        * Crea la tabla de usuarios en caso de que no exista
        *Los jugadores tienen un id, usuario y contraseña
        */
        String sql = "CREATE TABLE IF NOT EXISTS jugadores (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                     "usuario TEXT NOT NULL UNIQUE, " +
                     "password TEXT NOT NULL)";
        try {
            this.conexion.createStatement().execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Devuelve TRUE si se registró, FALSE si ya existía el usuario
    public boolean registrarUsuario(String user, String pass) {
        String sql = "INSERT INTO jugadores(usuario, password) VALUES(?,?)";
        try {
            PreparedStatement pstmt = this.conexion.prepareStatement(sql);
            pstmt.setString(1, user);
            pstmt.setString(2, pass);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false; // Error por usuario ya existente
        }
    }

    // Devuelve TRUE si el usuario y contraseña son correctos
    public boolean validarUsuario(String user, String pass) {
        String sql = "SELECT * FROM jugadores WHERE usuario = ? AND password = ?";
        try {
            PreparedStatement pstmt = this.conexion.prepareStatement(sql);
            pstmt.setString(1, user);
            pstmt.setString(2, pass);
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // Si hay resultados, el login es correcto
        } catch (SQLException e) {
            return false;
        }
    }
}