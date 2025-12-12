package org.yourcompany.yourproject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yourcompany.yourproject.Cartas.src.main.java.Mazo.Jugador;

public class Sala implements NotificacionJuego {
    private int id;
    private List<HiloCliente> clientesConectados;
    private Map<String, HiloCliente> mapaNombreCliente; 
    
    // Referencia al motor (Lógica)
    private MotorJuego motor;
    
    // Variables de Red/Gestión
    private HiloCliente anfitrion;
    private Map<HiloCliente, Boolean> jugadoresListosParaReiniciar;
    private final long TIEMPO_ESPERA_FIN_JUEGO = 15000;

    public Sala(int id, HiloCliente creador) {
        this.id = id;
        this.anfitrion = creador;
        this.clientesConectados = new ArrayList<>();
        this.mapaNombreCliente = new HashMap<>();
        
        // Inicializamos el motor pasando 'this' para que nos pueda notificar
        this.motor = new MotorJuego(this);
        
        agregarJugador(creador);
    }

    // --- Métodos de Gestión de Sala ---
    public int getId() { return id; }
    
    public synchronized void agregarJugador(HiloCliente cliente) {
        if (motor.isJuegoIniciado()) {
            cliente.enviarMensaje("Error: El juego ya ha comenzado.");
            return;
        }
        String nombre = cliente.getUsuarioActual();
        if (mapaNombreCliente.containsKey(nombre)) return; // Ya está

        clientesConectados.add(cliente);
        mapaNombreCliente.put(nombre, cliente);
        
        // Delegamos al motor
        motor.agregarJugador(nombre);
        
        broadcast("LOBBY_UPDATE: " + nombre + " se ha unido.");
        if (!esAnfitrion(cliente)) {
            cliente.enviarMensaje("Esperando al anfitrión...");
        }
    }

    public synchronized void removerJugador(HiloCliente cliente) {
        String nombre = cliente.getUsuarioActual();
        if (clientesConectados.remove(cliente)) {
            mapaNombreCliente.remove(nombre);
            motor.removerJugador(nombre);
            broadcast("LOBBY_UPDATE: " + nombre + " ha salido.");
            
            // Reasignar anfitrión si se va
            if (cliente.equals(anfitrion) && !clientesConectados.isEmpty()) {
                anfitrion = clientesConectados.get(0);
                broadcast("INFO: Nuevo anfitrión: " + anfitrion.getUsuarioActual());
            }
        }
    }
    
    public boolean tieneJugador(HiloCliente cliente) {
        return mapaNombreCliente.containsKey(cliente.getUsuarioActual());
    }
    
    public boolean esAnfitrion(HiloCliente cliente) {
        return cliente.equals(this.anfitrion);
    }

    public boolean isJuegoIniciado() {
        return motor.isJuegoIniciado();
    }

    // --- Delegación de Acciones al Motor ---
    
    public void iniciarJuego() {
        motor.iniciarJuego();
    }

    public void procesarAccionJugador(HiloCliente cliente, String accion) {
        // Sala solo pasa el nombre del usuario al motor
        motor.procesarAccion(cliente.getUsuarioActual(), accion);
    }

    public void procesarSeleccionObjetivo(HiloCliente cliente, String victima) {
        motor.procesarSeleccionObjetivo(cliente.getUsuarioActual(), victima);
    }

    // --- Persistencia (Sala coordina DB y Motor) ---
    
    public synchronized void guardarPartida(BaseDeDatos db, HiloCliente solicitante) {
        if (!esAnfitrion(solicitante)) {
            solicitante.enviarMensaje("Error: Solo el anfitrión guarda.");
            return;
        }
        // Pedimos los datos puros al motor
        List<Jugador> listaJugadores = motor.getJugadores();
        int idGuardado = db.guardarPartida(listaJugadores);

        if (idGuardado != -1) {
            broadcast("SISTEMA: Partida guardada ID: " + idGuardado);
        } else {
            solicitante.enviarMensaje("Error al guardar.");
        }
    }
    
    public void establecerDatosCargados(Map<String, Integer> datos) {
        motor.cargarPuntajesPrevios(datos);
    }

    // --- Votación Fin de Juego (Lógica de Sala) ---
    // Esta parte se queda en Sala porque implica desconexiones y sockets
    public synchronized void procesarVoto(HiloCliente cliente, String comando) {
        if (motor.isJuegoIniciado()) return; // Solo votar si acabó el juego

        if (jugadoresListosParaReiniciar != null && jugadoresListosParaReiniciar.containsKey(cliente)) {
            cliente.enviarMensaje("Ya has votado.");
            return;
        }
        
        if (comando.equals("REINICIAR")) {
            if(jugadoresListosParaReiniciar == null) jugadoresListosParaReiniciar = new HashMap<>();
            jugadoresListosParaReiniciar.put(cliente, true);
            broadcast("VOTO: " + cliente.getUsuarioActual() + " votó REINICIAR.");
        } else if (comando.equals("SALIR_SALA") || comando.equals("VOTAR_SALIR")) {
            removerJugador(cliente);
            cliente.enviarMensaje("Saliste de la sala.");
        }

        if (jugadoresListosParaReiniciar != null && clientesConectados.size() == jugadoresListosParaReiniciar.size()) {
            terminarVotacion();
        }
    }

    private synchronized void terminarVotacion() {
        List<HiloCliente> aRemover = new ArrayList<>();
        for (HiloCliente c : clientesConectados) {
            if (jugadoresListosParaReiniciar == null || !jugadoresListosParaReiniciar.containsKey(c)) {
                aRemover.add(c);
                c.enviarMensaje("Te sacaron por no votar reiniciar.");
            }
        }
        for (HiloCliente c : aRemover) removerJugador(c);

        if (clientesConectados.size() >= 2) {
            broadcast("Reiniciando...");
            motor.iniciarJuego();
        } else {
            broadcast("No hay suficientes jugadores.");
        }
        this.jugadoresListosParaReiniciar = null;
    }

    // --- Implementación de NotificacionJuego ---
    @Override
    public void enviarMensajePrivado(String nombreUsuario, String mensaje) {
        HiloCliente c = mapaNombreCliente.get(nombreUsuario);
        if (c != null) c.enviarMensaje(mensaje);
    }

    @Override
    public void broadcast(String mensaje) {
        for (HiloCliente c : clientesConectados) c.enviarMensaje(mensaje);
    }

    @Override
    public void onFinDeJuego(String ganador) {
        broadcast("!!! JUEGO TERMINADO !!! Ganador: " + ganador);
        broadcast("Escriban REINICIAR o VOTAR_SALIR");
        
        // Iniciamos timer de votación
        jugadoresListosParaReiniciar = new HashMap<>();
        new Thread(() -> {
            try { Thread.sleep(TIEMPO_ESPERA_FIN_JUEGO); synchronized(this){ terminarVotacion(); } } 
            catch (InterruptedException e) {}
        }).start();
    }
}