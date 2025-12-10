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

        String sqlPartidas = "CREATE TABLE IF NOT EXISTS partidas (" +
                 "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                 "fecha DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                 "estado TEXT)";

    String sqlDetalles = "CREATE TABLE IF NOT EXISTS partida_detalles (" +
                 "id_partida INTEGER, " +
                 "usuario TEXT, " +
                 "puntaje_total INTEGER, " +
                 "FOREIGN KEY(id_partida) REFERENCES partidas(id))";
        try {
            this.conexion.createStatement().execute(sql);
            this.conexion.createStatement().execute(sqlPartidas);
            this.conexion.createStatement().execute(sqlDetalles);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int guardarPartida(java.util.List<org.yourcompany.yourproject.Cartas.src.main.java.Mazo.Jugador> listaJugadores) {
    int idPartida = -1;
    try {
        // 1. Crear el registro de la partida
        String sqlPartida = "INSERT INTO partidas(estado) VALUES('GUARDADA')";
        PreparedStatement pstmt = this.conexion.prepareStatement(sqlPartida, java.sql.Statement.RETURN_GENERATED_KEYS);
        pstmt.executeUpdate();
        
        ResultSet rs = pstmt.getGeneratedKeys();
        if (rs.next()) {
            idPartida = rs.getInt(1);
        }

        // 2. Guardar a cada jugador y su puntaje
        if (idPartida != -1) {
            String sqlDetalle = "INSERT INTO partida_detalles(id_partida, usuario, puntaje_total) VALUES(?,?,?)";
            PreparedStatement pstmtDetalle = this.conexion.prepareStatement(sqlDetalle);

            for (org.yourcompany.yourproject.Cartas.src.main.java.Mazo.Jugador j : listaJugadores) {
                pstmtDetalle.setInt(1, idPartida);
                pstmtDetalle.setString(2, j.getNombre());
                pstmtDetalle.setInt(3, j.getPuntajeTotal());
                pstmtDetalle.addBatch(); // Agrupamos las inserciones
            }
            pstmtDetalle.executeBatch();
        }

        } catch (SQLException e) {
            System.out.println("Error al guardar partida: " + e.getMessage());
            return -1;
        }
        return idPartida;
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
    // En BaseDeDatos.java

public java.util.Map<String, Integer> cargarDatosPartida(int idPartida) {
    java.util.Map<String, Integer> puntajes = new java.util.HashMap<>();
    
    // Buscamos los detalles de esa partida específica
    String sql = "SELECT usuario, puntaje_total FROM partida_detalles WHERE id_partida = ?";
    
    try {
        PreparedStatement pstmt = this.conexion.prepareStatement(sql);
        pstmt.setInt(1, idPartida);
        ResultSet rs = pstmt.executeQuery();
        
        while (rs.next()) {
            // Guardamos: Nombre -> Puntaje
            puntajes.put(rs.getString("usuario"), rs.getInt("puntaje_total"));
        }
    } catch (SQLException e) {
        System.out.println("Error al cargar partida: " + e.getMessage());
    }
    return puntajes;
}
}