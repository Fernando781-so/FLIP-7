package org.yourcompany.yourproject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yourcompany.yourproject.Cartas.src.main.java.Mazo.Carta;
import org.yourcompany.yourproject.Cartas.src.main.java.Mazo.Jugador;
import org.yourcompany.yourproject.Cartas.src.main.java.Mazo.Mazo;
import org.yourcompany.yourproject.Cartas.src.main.java.Mazo.TipoAccion;

public class Sala {
    private int id;
    private List<HiloCliente> clientesConectados;
    private Map<String, HiloCliente> mapaNombreCliente; 
    private Map<HiloCliente, Jugador> mapaEstadoJugador; 

    private TipoAccion accionPendiente = null; 
    private HiloCliente jugadorPendienteDeAccion = null;
    
    private final long TIEMPO_ESPERA_FIN_JUEGO = 15000; // 15 segundos
    private Map<HiloCliente, Boolean> jugadoresListosParaReiniciar; // Almacena qué jugadores quieren reiniciar

    private Mazo mazo;
    private boolean juegoIniciado;
    private int indiceTurnoActual;
    private int numeroRonda = 1; // Para llevar control

    public Sala(int id, HiloCliente creador) {
        this.id = id;
        this.clientesConectados = new ArrayList<>();
        this.mapaNombreCliente = new HashMap<>();
        this.mapaEstadoJugador = new HashMap<>();
        this.juegoIniciado = false;
        agregarJugador(creador);
    }

    // ... (Métodos getId, tieneJugador, agregarJugador y removerJugador IGUALES QUE ANTES) ...
    public int getId() { return id; }
    public boolean tieneJugador(HiloCliente cliente) { return clientesConectados.contains(cliente); }

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
        cliente.enviarMensaje("Esperando al anfitrión... (Envía INICIAR_PARTIDA)");
    }
    
    public synchronized void removerJugador(HiloCliente cliente) {
        if (clientesConectados.remove(cliente)) {
            String nombre = cliente.getUsuarioActual();
            mapaNombreCliente.remove(nombre);
            mapaEstadoJugador.remove(cliente);
            broadcast("LOBBY_UPDATE: " + nombre + " ha abandonado la sala.");
        }
    }


    public synchronized void iniciarJuego() {
    if (clientesConectados.size() < 2) { 
        broadcast("Error: Se necesitan al menos 2 jugadores.");
        return;
    }
    this.juegoIniciado = true;

    broadcast("JUEGO_INICIADO"); 

    this.numeroRonda = 1;
    iniciarNuevaRonda();
}
    private void iniciarNuevaRonda() {
        this.mazo = new Mazo(); 
        this.mazo.barajar();
        this.indiceTurnoActual = 0;

        // Reiniciar estados de ronda de los jugadores
        for (Jugador j : mapaEstadoJugador.values()) {
            j.reiniciarRonda();
        }

        broadcast("------------------------------------------------");
        broadcast("INICIO_RONDA: Ronda #" + numeroRonda);
        broadcast("------------------------------------------------");
        notificarTurno();
    }

    private void notificarTurno() {
        // Verificar primero si la ronda debe terminar (todos plantados o eliminados)
        if (verificarFinDeRondaAutomatico()) {
            finalizarRonda(null); // Null porque nadie ganó por 7 cartas, se acabó por desgaste
            return;
        }

        HiloCliente clienteActual = clientesConectados.get(indiceTurnoActual);
        Jugador jugador = mapaEstadoJugador.get(clienteActual);

        // Si el jugador actual está plantado o eliminado, saltamos al siguiente
        if (jugador.isPlantado() || jugador.isEliminadoRonda()) {
            siguienteTurno();
            return;
        }

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

    if (accionPendiente != null && cliente.equals(jugadorPendienteDeAccion)) {
        cliente.enviarMensaje("Error: Tienes una acción (" + accionPendiente.toString() + ") pendiente de resolver. Debes enviar el comando de SELECCIONAR:Nombre.");
        return;
    }

    Jugador jugador = mapaEstadoJugador.get(cliente);

    if (accion.equals("PLANTARSE")) {
        jugador.setPlantado(true);
        broadcast("JUEGO: " + jugador.getNombre() + " se planta con " + jugador.getPuntajeRonda() + " puntos.");
        siguienteTurno(); 
    } 
    else if (accion.equals("ROBAR")) {
        Carta carta = mazo.tomarCarta(); 
        if (carta == null) {
            broadcast("JUEGO: Se acabó el mazo.");
            finalizarRonda(null);
            return;
        }
        broadcast("JUEGO: " + jugador.getNombre() + " sacó " + carta.toString());
        procesarCartaSacada(cliente, jugador, carta); 
    } else {
        cliente.enviarMensaje("Error: Acción no válida. Usa ROBAR o PLANTARSE.");
    }
}

    private void procesarCartaSacada(HiloCliente cliente, Jugador jugador, Carta carta) {
        // 1. Verificar si explotó (tiene el mismo número)
        if (carta.getTipo() == TipoAccion.NUMERO) {
            boolean tieneCarta = false;
            for (Carta c : jugador.getMano()) {
                if (c.getTipo() == TipoAccion.NUMERO && c.getValor() == carta.getValor()) {
                    tieneCarta = true; break;
                }
            }

            if (tieneCarta) {
                // Lógica de explosión
                if (tieneSecondChance(jugador)) {
                     broadcast("JUEGO: " + jugador.getNombre() + " usa SECOND CHANCE y se salva.");
                     eliminarSecondChance(jugador);
                     jugador.recibirCarta(carta);
                     if(!verificarCondicionesVictoriaRonda(cliente, jugador)) {
                         siguienteTurno(); // Solo pasamos turno si no ganó la ronda
                     }
                } else {
                     broadcast("EXPLOSION: " + jugador.getNombre() + " explotó con un " + carta.getValor());
                     jugador.setPuntajeRonda(0);
                     jugador.getMano().clear();
                     jugador.setEliminadoRonda(true); // Ya no juega en esta ronda
                     siguienteTurno();
                }
            } else {
                // No explotó, agregamos carta
                jugador.recibirCarta(carta);
                jugador.calcularPuntajeRonda();
                cliente.enviarMensaje("ESTADO: Mano: " + jugador.getMano().toString());
                
                // Verificar si ganó la ronda por 7 cartas
                if (!verificarCondicionesVictoriaRonda(cliente, jugador)) {
                    siguienteTurno(); // Solo pasamos turno si no ganó la ronda
                }
            }
            return;
        }

        // Si es carta especial
        manejarCartaAccion(cliente, jugador, carta);
    }
    
    // Verifica si alguien ganó la RONDA o si se acaba la ronda
    private boolean verificarCondicionesVictoriaRonda(HiloCliente cliente, Jugador jugador) {
        // Regla: Tener 7 cartas numéricas
        if (jugador.getCantidadNumericas() >= 7) {
            broadcast("!!! BONIFICACIÓN !!! " + jugador.getNombre() + " ha juntado 7 cartas numéricas.");
            // Opcional: Dar bonificación de puntos extra aquí si quieres
            finalizarRonda(jugador); // Este jugador provocó el fin
            return true;
        }
        return false;
    }

    private boolean verificarFinDeRondaAutomatico() {
        // La ronda termina si TODOS están plantados o eliminados
        for (Jugador j : mapaEstadoJugador.values()) {
            if (!j.isPlantado() && !j.isEliminadoRonda()) {
                return false; // Aún queda alguien activo
            }
        }
        return true;
    }

    private void finalizarRonda(Jugador ganadorPorSiete) {
        broadcast("\n=== FIN DE LA RONDA " + numeroRonda + " ===");
        
        StringBuilder resumen = new StringBuilder("Puntajes Ronda:\n");
        boolean hayGanadorJuego = false;
        String nombreGanador = "";

        for (HiloCliente hc : clientesConectados) {
            Jugador j = mapaEstadoJugador.get(hc);
            
            // Sumamos los puntos de esta ronda al total
            j.agregarPuntajeTotal(j.getPuntajeRonda());
            
            resumen.append(j.getNombre())
                   .append(": +").append(j.getPuntajeRonda())
                   .append(" (Total: ").append(j.getPuntajeTotal()).append(")\n");

            // Regla: 200 Puntos para ganar la partida
            if (j.getPuntajeTotal() >= 200) {
                hayGanadorJuego = true;
                nombreGanador = j.getNombre();
            }
        }
        broadcast(resumen.toString());

        if (hayGanadorJuego) {
            broadcast("\n********************************************");
            broadcast("!!! FIN DEL JUEGO !!!");
            broadcast("GANADOR: " + nombreGanador);
            broadcast("********************************************");
            this.juegoIniciado = false;
            // Aquí podrías reiniciar todo o cerrar la sala
            
        } else {
            // Preparamos siguiente ronda
            numeroRonda++;
            // Damos unos segundos para leer puntajes
            try { Thread.sleep(3000); } catch (InterruptedException e) {}
            iniciarNuevaRonda();
        }
        
    }

    // --- MÉTODOS DE CARTAS ESPECIALES (Con ajustes para cambio de turno) ---

    private void manejarCartaAccion(HiloCliente cliente, Jugador jugador, Carta carta) {
        jugador.recibirCarta(carta); 

        switch (carta.getTipo()) {
            case FREEZE:
                this.accionPendiente = TipoAccion.FREEZE;
                this.jugadorPendienteDeAccion = cliente;
                String candidatosFreeze = obtenerListaCandidatos(cliente.getUsuarioActual(), false);
                cliente.enviarMensaje("SELECCIONAR:FREEZE:¿A quién quieres congelar?:" + candidatosFreeze);
                // NOTA: No llamamos a siguienteTurno() aquí, esperamos la respuesta del jugador
                break;
                
            case FLIP_THREE:
                this.accionPendiente = TipoAccion.FLIP_THREE;
                this.jugadorPendienteDeAccion = cliente;
                String candidatosFlip = obtenerListaCandidatos(cliente.getUsuarioActual(), true);
                cliente.enviarMensaje("SELECCIONAR:FLIP_THREE:¿A quién atacas con 3 cartas?:" + candidatosFlip);
                break;
                
            case SECOND_CHANCE:
                broadcast("ACCION: " + jugador.getNombre() + " obtuvo Second Chance.");
                siguienteTurno(); // Pasiva, pasa el turno
                break;
        }
    }

 public synchronized void procesarSeleccionObjetivo(HiloCliente cliente, String nombreVictima) {
    // 1. Verificar si hay una acción pendiente y si el cliente es el correcto
    if (accionPendiente == null || !cliente.equals(jugadorPendienteDeAccion)) {
        cliente.enviarMensaje("Error: No se espera una selección de objetivo o no es tu turno para seleccionar.");
        return;
    }
    
    HiloCliente victima = mapaNombreCliente.get(nombreVictima);
    if (victima == null) {
        cliente.enviarMensaje("Error: El jugador seleccionado no existe o no está en la sala.");
        return;
    }
    
    // 2. Ejecutar la acción
    if (accionPendiente == TipoAccion.FREEZE) {
        if (nombreVictima.equals(cliente.getUsuarioActual())) {
            cliente.enviarMensaje("Error: No puedes congelarte a ti mismo.");
            return; 
        }
        
        Jugador jugVictima = mapaEstadoJugador.get(victima);
        broadcast("FREEZE: " + cliente.getUsuarioActual() + " congela a " + nombreVictima);
        
        // La víctima queda plantada/inactiva para esta ronda
        jugVictima.setPlantado(true); 
        broadcast("EFECTO: " + nombreVictima + " ha quedado congelado (No puede robar más en esta ronda).");
        
        // Limpiar estado
        accionPendiente = null;
        jugadorPendienteDeAccion = null;
        siguienteTurno(); 

    } else if (accionPendiente == TipoAccion.FLIP_THREE) {
        // En el caso de FLIP_THREE, el método aplicado ya gestiona el pase de turno.
        broadcast("FLIP_THREE: " + cliente.getUsuarioActual() + " ataca a " + nombreVictima);
        aplicarFlipThree(cliente, nombreVictima);
        
        // Limpiar estado
        accionPendiente = null;
        jugadorPendienteDeAccion = null;
        // aplicarFlipThree() llama a siguienteTurno() al finalizar.
    }
}

    public synchronized void aplicarFlipThree(HiloCliente atacante, String nombreVictima) {
        HiloCliente clienteVictima = mapaNombreCliente.get(nombreVictima);
        Jugador jugVictima = mapaEstadoJugador.get(clienteVictima);

        for (int i = 0; i < 3; i++) {
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
            Carta c = mazo.tomarCarta();
            if (c == null) break;
            
            broadcast("FLIP_THREE: " + nombreVictima + " voltea... " + c.toString());
            procesarCartaForzada(clienteVictima, jugVictima, c);
            
            if (jugVictima.isEliminadoRonda()) break; // Si explotó, para.
        }
        siguienteTurno();
    }
    
    // Métodos auxiliares sin cambios mayores
    private void procesarCartaForzada(HiloCliente cliente, Jugador jugador, Carta carta) {
        // Lógica similar a procesarCartaSacada pero sin pasar turno
        boolean tieneCarta = false;
        if(carta.getTipo() == TipoAccion.NUMERO) {
             for (Carta c : jugador.getMano()) {
                if (c.getTipo() == TipoAccion.NUMERO && c.getValor() == carta.getValor()) {
                    tieneCarta = true; break;
                }
            }
        }

        if (tieneCarta) {
             if (tieneSecondChance(jugador)) {
                 broadcast("JUEGO: " + jugador.getNombre() + " se salva con SECOND CHANCE.");
                 eliminarSecondChance(jugador);
                 jugador.recibirCarta(carta);
             } else {
                 broadcast("EXPLOSION: " + jugador.getNombre() + " explotó en el ataque.");
                 jugador.setPuntajeRonda(0);
                 jugador.getMano().clear();
                 jugador.setEliminadoRonda(true);
             }
        } else {
             jugador.recibirCarta(carta);
             jugador.calcularPuntajeRonda();
        }
    }

    private String obtenerListaNombresJugadores() { /* Igual que antes */ 
        StringBuilder sb = new StringBuilder();
        for(String s : mapaNombreCliente.keySet()) sb.append(s).append(",");
        return sb.toString();
    }
    private boolean tieneSecondChance(Jugador j) { /* Igual que antes */ 
        for(Carta c : j.getMano()) if(c.getTipo() == TipoAccion.SECOND_CHANCE) return true;
        return false;
    }
    private void eliminarSecondChance(Jugador j) { /* Igual que antes */
         for(int i=0; i<j.getMano().size(); i++){
             if(j.getMano().get(i).getTipo() == TipoAccion.SECOND_CHANCE){ j.getMano().remove(i); return; }
         }
    }
    private String obtenerListaCandidatos(String miNombre, boolean incluirme) { /* Igual que antes */
        StringBuilder sb = new StringBuilder();
        for(String n : mapaNombreCliente.keySet()){ if(!incluirme && n.equals(miNombre)) continue; sb.append(n).append(","); }
        return sb.toString();
    }
    public void broadcast(String mensaje) {
        for (HiloCliente c : clientesConectados) {
            // Usamos el método enviarMensaje que ya tiene HiloCliente
            c.enviarMensaje(mensaje); 
        }
    }
    public boolean isJuegoIniciado() {
    return juegoIniciado;
}


}