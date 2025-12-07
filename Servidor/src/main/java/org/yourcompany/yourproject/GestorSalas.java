package org.yourcompany.yourproject;

import java.util.HashMap;
import java.util.Map;

public class GestorSalas {
    private static Map<Integer, Sala> salas = new HashMap<>();
    private static int contadorIds = 1;

    public static synchronized int crearSala(HiloCliente creador) {
        int id = contadorIds++;
        Sala nuevaSala = new Sala(id, creador);
        salas.put(id, nuevaSala);
        return id;
    }

    public static synchronized Sala obtenerSala(int id) {
        return salas.get(id);
    }
    

    public static synchronized String listarSalas() {
        if (salas.isEmpty()) return "No hay salas disponibles.";
        StringBuilder sb = new StringBuilder();
        for (Integer id : salas.keySet()) {
            sb.append("Sala ").append(id).append(", ");
        }
        return sb.toString();
    }
    public static synchronized Sala buscarSalaDeJugador(HiloCliente cliente) {
    for (Sala s : salas.values()) { 
        if (s.tieneJugador(cliente)) {
            return s; 
        }
    }
    return null; 
}
}