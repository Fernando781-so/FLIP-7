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
    private Map<String, HiloCliente> mapaNombreCliente; 
    private Map<HiloCliente, Jugador> mapaEstadoJugador; 

    private TipoAccion accionPendiente = null; 
    private HiloCliente jugadorPendienteDeAccion = null;
    
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

        broadcast("LOBBY_UPDATE:Jugadores en sala: " + obtenerListaNombresJugadores());
        cliente.enviarMensaje("Esperando al anfitrión para iniciar... (Si eres tú, envía INICIAR_PARTIDA)");
    }

    public synchronized void iniciarJuego() {
        if (clientesConectados.size() < 2) { 
            broadcast("Error: Se necesitan al menos 2 jugadores.");
            return;
        }
        this.juegoIniciado = true;
        this.mazo = new Mazo(); 
        this.mazo.barajar();
        this.indiceTurnoActual = 0;
        
        // Repartir carta inicial o mostrar estado
        broadcast("JUEGO_INICIADO: La partida comienza.");
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
    public boolean tieneJugador(HiloCliente cliente) {
    return clientesConectados.contains(cliente);
}

    private void procesarCartaSacada(HiloCliente cliente, Jugador jugador, Carta carta) {
        // Si es número, lógica normal de explosión (igual que antes)
        if (carta.getTipo() == TipoAccion.NUMERO) {
            boolean tieneCarta = false;
            for (Carta c : jugador.getMano()) {
                if (c.getTipo() == TipoAccion.NUMERO && c.getValor() == carta.getValor()) {
                    tieneCarta = true; break;
                }
            }
            if (tieneCarta) {
 
                if (tieneSecondChance(jugador)) {
                     broadcast("JUEGO: " + jugador.getNombre() + " se salva con SECOND CHANCE.");
                     eliminarSecondChance(jugador);
                     jugador.recibirCarta(carta);

                     cliente.enviarMensaje("TU_TURNO: ¿Seguir robando o Plantarse?");
                } else {
                     broadcast("EXPLOSION: " + jugador.getNombre() + " explotó con un " + carta.getValor());
                     jugador.setPuntaje(0);
                     jugador.getMano().clear();
                     siguienteTurno();
                }
            } else {
                jugador.recibirCarta(carta);
                jugador.calcularPuntaje();
                cliente.enviarMensaje("ESTADO: Mano: " + jugador.getMano().toString());
                cliente.enviarMensaje("TU_TURNO: ¿Seguir robando o Plantarse?");
            }
            return;
        }


        manejarCartaAccion(cliente, jugador, carta);
    }


private void manejarCartaAccion(HiloCliente cliente, Jugador jugador, Carta carta) {
        jugador.recibirCarta(carta); // La carta se añade a la mano (o al descarte, depende tus reglas)

        switch (carta.getTipo()) {
            case FREEZE:
                // Guardamos estado: Esperamos que este cliente elija objetivo para Freeze
                this.accionPendiente = TipoAccion.FREEZE;
                this.jugadorPendienteDeAccion = cliente;
                
                // Enviamos lista de candidatos (TODOS MENOS UNO MISMO)
                String candidatosFreeze = obtenerListaCandidatos(cliente.getUsuarioActual(), false);
                cliente.enviarMensaje("SELECCIONAR:FREEZE:¿A quién quieres congelar?:" + candidatosFreeze);
                break;
                
            case FLIP_THREE:
                this.accionPendiente = TipoAccion.FLIP_THREE;
                this.jugadorPendienteDeAccion = cliente;
                
                // Enviamos lista de candidatos (TODOS INCLUYENDOSE A SI MISMO)
                String candidatosFlip = obtenerListaCandidatos(cliente.getUsuarioActual(), true);
                cliente.enviarMensaje("SELECCIONAR:FLIP_THREE:¿A quién atacas con 3 cartas?:" + candidatosFlip);
                break;
                
            case SECOND_CHANCE:
                // Esta es pasiva, no requiere objetivo, simplemente se guarda
                broadcast("ACCION: " + jugador.getNombre() + " obtuvo Second Chance.");
                cliente.enviarMensaje("TU_TURNO: ¿Seguir robando o Plantarse?");
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
public synchronized void procesarSeleccionObjetivo(HiloCliente cliente, String nombreVictima) {
        // Validaciones de seguridad
        if (!cliente.equals(jugadorPendienteDeAccion) || accionPendiente == null) {
            cliente.enviarMensaje("Error: No se espera ninguna acción tuya.");
            return;
        }

        HiloCliente victima = mapaNombreCliente.get(nombreVictima);
        if (victima == null) {
            cliente.enviarMensaje("Error: Jugador no válido.");
            return;
        }

        // Ejecutar la acción pendiente
        if (accionPendiente == TipoAccion.FREEZE) {
            if (nombreVictima.equals(cliente.getUsuarioActual())) {
                cliente.enviarMensaje("Error: No puedes congelarte a ti mismo.");
                return; 
            }
            broadcast("FREEZE: " + cliente.getUsuarioActual() + " ha congelado a " + nombreVictima);
            // Lógica de efecto Freeze: Termina turno del atacante y quizás marca a la víctima para perder turno
            accionPendiente = null;
            jugadorPendienteDeAccion = null;
            siguienteTurno(); 

        } else if (accionPendiente == TipoAccion.FLIP_THREE) {
            // Aquí si permite atacarse a uno mismo
            broadcast("FLIP_THREE: " + cliente.getUsuarioActual() + " obliga a robar 3 cartas a " + nombreVictima);
            
            aplicarFlipThree(cliente, nombreVictima); // Tu método existente
            
            accionPendiente = null;
            jugadorPendienteDeAccion = null;
            // aplicarFlipThree ya llama a siguienteTurno() al final
        }
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

    private String obtenerListaCandidatos(String miNombre, boolean incluirme) {
        StringBuilder sb = new StringBuilder();
        for (String nombre : mapaNombreCliente.keySet()) {
            if (!incluirme && nombre.equals(miNombre)) continue; // Saltar si no debo incluirme
            sb.append(nombre).append(",");
        }
        return sb.toString();
    }
    
    // Método helper necesario en GestorSalas
    /* public static Sala buscarSalaDeJugador(HiloCliente cliente) {
        for(Sala s : salas.values()) {
             if(s.tieneJugador(cliente)) return s; // Necesitas implementar tieneJugador en Sala
        }
        return null;
    }
    */
}
