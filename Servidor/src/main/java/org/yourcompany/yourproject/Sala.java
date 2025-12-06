package org.yourcompany.yourproject;

import org.yourcompany.yourproject.Cartas.src.main.java.Mazo.Carta;
import org.yourcompany.yourproject.Cartas.src.main.java.Mazo.Jugador;
import org.yourcompany.yourproject.Cartas.src.main.java.Mazo.Mazo;
import org.yourcompany.yourproject.Cartas.src.main.java.Mazo.TipoAccion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sala {
    private int id;
    private List<HiloCliente> clientesConectados;
    private Map<String, HiloCliente> mapaNombreCliente; // Para buscar rápido por nombre
    private Map<HiloCliente, Jugador> mapaEstadoJugador; // Vincula la conexión con el estado del juego
    
    private Mazo mazo;
    private boolean juegoIniciado;
    private int indiceTurnoActual;

    public Sala(int id, HiloCliente creador) {
        this.id = id;
        this.clientesConectados = new ArrayList<>();
        this.mapaNombreCliente = new HashMap<>();
        this.mapaEstadoJugador = new HashMap<>();
        this.juegoIniciado = false;
        
        agregarJugador(creador);
    }

    public int getId() { return id; }
    
    public boolean isJuegoIniciado() { return juegoIniciado; }

    public synchronized void agregarJugador(HiloCliente cliente) {
        if (juegoIniciado) {
            cliente.enviarMensaje("Error: El juego ya ha comenzado.");
            return;
        }
        
        clientesConectados.add(cliente);
        String nombre = cliente.getUsuarioActual(); 
        mapaNombreCliente.put(nombre, cliente);
        
        
        Jugador nuevoJugador = new Jugador(nombre); 
        mapaEstadoJugador.put(cliente, nuevoJugador);

        broadcast("SALA: El jugador " + nombre + " se ha unido a la sala " + id);
    }

    public synchronized void iniciarJuego() {
        if (clientesConectados.size() < 2) { 


        }
        
        this.juegoIniciado = true;
        this.mazo = new Mazo(); 
        this.mazo.barajar();
        this.indiceTurnoActual = 0;

        broadcast("JUEGO_INICIADO: La partida ha comenzado.");
        notificarTurno();
    }



    private void notificarTurno() {
        HiloCliente clienteActual = clientesConectados.get(indiceTurnoActual);
        broadcast("TURNO: Es el turno de " + clienteActual.getUsuarioActual());
        clienteActual.enviarMensaje("TU_TURNO: ¿Qué deseas hacer? (ROBAR / PLANTARSE)");
    }

    public synchronized void siguienteTurno() {
        indiceTurnoActual = (indiceTurnoActual + 1) % clientesConectados.size();
        notificarTurno();
    }



    public synchronized void procesarAccionJugador(HiloCliente cliente, String accion) {
        if (!clientesConectados.get(indiceTurnoActual).equals(cliente)) {
            cliente.enviarMensaje("Error: No es tu turno.");
            return;
        }

        Jugador jugador = mapaEstadoJugador.get(cliente);

        if (accion.equals("PLANTARSE")) {
            broadcast("JUEGO: " + jugador.getNombre() + " se planta con " + jugador.getPuntaje() + " puntos.");
            siguienteTurno();
        } 
        else if (accion.equals("ROBAR")) {
            Carta carta = mazo.tomarCarta(); //
            
            if (carta == null) {
                broadcast("JUEGO: Se acabó el mazo. Fin de la ronda.");

                return;
            }

            broadcast("JUEGO: " + jugador.getNombre() + " sacó " + carta.toString());
            procesarCartaSacada(cliente, jugador, carta);
        }
    }

    private void procesarCartaSacada(HiloCliente cliente, Jugador jugador, Carta carta) {

        if (carta.getTipo() != TipoAccion.NUMERO) {
            manejarCartaAccion(cliente, jugador, carta);
            return;
        }


        boolean tieneCarta = false;
        for (Carta c : jugador.getMano()) {
            if (c.getTipo() == TipoAccion.NUMERO && c.getValor() == carta.getValor()) {
                tieneCarta = true;
                break;
            }
        }

        if (tieneCarta) {

            if (tieneSecondChance(jugador)) {
                broadcast("JUEGO: ¡" + jugador.getNombre() + " usa SECOND CHANCE y se salva!");
                eliminarSecondChance(jugador);
                jugador.recibirCarta(carta); 

            } else {
                broadcast("EXPLOSION: " + jugador.getNombre() + " ha sacado un " + carta.getValor() + " repetido y pierde sus puntos.");
                jugador.setPuntaje(0); 
                jugador.getMano().clear(); 
                siguienteTurno();
            }
        } else {
            jugador.recibirCarta(carta);
            jugador.calcularPuntaje(); 
            cliente.enviarMensaje("ESTADO: Tu mano actual: " + jugador.getMano().toString());
            cliente.enviarMensaje("TU_TURNO: ¿Seguir robando o Plantarse?");
        }
    }


    private void manejarCartaAccion(HiloCliente cliente, Jugador jugador, Carta carta) {
        switch (carta.getTipo()) {
            case FREEZE:
                broadcast("ACCION: " + jugador.getNombre() + " se congela (FREEZE). Termina su turno seguro.");
                jugador.recibirCarta(carta); 
                siguienteTurno();
                break;
                
            case SECOND_CHANCE:
                broadcast("ACCION: " + jugador.getNombre() + " obtiene una SECOND CHANCE.");
                jugador.recibirCarta(carta);

                cliente.enviarMensaje("TU_TURNO: ¿Seguir robando o Plantarse?");
                break;
                
            case FLIP_THREE:
                jugador.recibirCarta(carta);

                cliente.enviarMensaje("SELECCIONAR_OBJETIVO:" + obtenerListaNombresJugadores());
                break;
        }
    }


    public synchronized void aplicarFlipThree(HiloCliente atacante, String nombreVictima) {
        HiloCliente clienteVictima = mapaNombreCliente.get(nombreVictima);
        if (clienteVictima == null) {
            atacante.enviarMensaje("Error: Jugador no encontrado.");
            return;
        }

        Jugador jugVictima = mapaEstadoJugador.get(clienteVictima);
        broadcast("FLIP_THREE: " + atacante.getUsuarioActual() + " obliga a " + nombreVictima + " a voltear 3 cartas.");


        for (int i = 0; i < 3; i++) {
            try { Thread.sleep(1000); } catch (InterruptedException e) {} // Pausa dramática
            Carta c = mazo.tomarCarta();
            if (c == null) break;
            
            broadcast("FLIP_THREE: " + nombreVictima + " voltea... " + c.toString());
            

            procesarCartaForzada(clienteVictima, jugVictima, c);
            

            if (jugVictima.getMano().isEmpty() && jugVictima.getPuntaje() == 0) {
                break; 
            }
        }
        

        siguienteTurno();
    }


    private void procesarCartaForzada(HiloCliente cliente, Jugador jugador, Carta carta) {

        boolean tieneCarta = false;
        for (Carta c : jugador.getMano()) {
            if (c.getTipo() == TipoAccion.NUMERO && c.getValor() == carta.getValor()) {
                tieneCarta = true; break;
            }
        }
        if (tieneCarta) {
             broadcast("EXPLOSION: " + jugador.getNombre() + " explotó durante el Flip Three.");
             jugador.setPuntaje(0);
             jugador.getMano().clear();
        } else {
             jugador.recibirCarta(carta);
             jugador.calcularPuntaje();
        }
    }



    public void broadcast(String mensaje) {
        for (HiloCliente c : clientesConectados) {
            c.enviarMensaje(mensaje); 
        }
    }

    private String obtenerListaNombresJugadores() {
        StringBuilder sb = new StringBuilder();
        for (String nombre : mapaNombreCliente.keySet()) {
            sb.append(nombre).append(",");
        }
        return sb.toString();
    }

    private boolean tieneSecondChance(Jugador j) {
        for (Carta c : j.getMano()) {
            if (c.getTipo() == TipoAccion.SECOND_CHANCE) return true;
        }
        return false;
    }

    private void eliminarSecondChance(Jugador j) {
        for (int i = 0; i < j.getMano().size(); i++) {
            if (j.getMano().get(i).getTipo() == TipoAccion.SECOND_CHANCE) {
                j.getMano().remove(i);
                return;
            }
        }
    }
}
