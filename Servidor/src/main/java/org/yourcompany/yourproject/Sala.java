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
    
    // NUEVO: Variable para almacenar los puntajes que vienen de la base de datos al cargar
    private Map<String, Integer> puntajesGuardados = null; 

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

    public int getId() { return id; }
    public boolean tieneJugador(HiloCliente cliente) { return clientesConectados.contains(cliente); }

    // NUEVO: Método para recibir los datos desde el procesador al cargar partida
    public void establecerDatosCargados(Map<String, Integer> datos) {
        this.puntajesGuardados = datos;
        // Si el anfitrión ya está dentro al momento de cargar, actualizamos su puntaje inmediatamente
        for (HiloCliente cliente : clientesConectados) {
            String nombre = cliente.getUsuarioActual();
            if (puntajesGuardados.containsKey(nombre)) {
                Jugador j = mapaEstadoJugador.get(cliente);
                j.agregarPuntajeTotal(puntajesGuardados.get(nombre));
            }
        }
    }

    public synchronized void agregarJugador(HiloCliente cliente) {
        if (juegoIniciado) {
            cliente.enviarMensaje("Error: El juego ya ha comenzado.");
            return;
        }
        clientesConectados.add(cliente);
        String nombre = cliente.getUsuarioActual(); 
        mapaNombreCliente.put(nombre, cliente);
        
        Jugador nuevoJugador = new Jugador(nombre); 
        
        // --- INICIO LÓGICA DE CARGA ---
        if (puntajesGuardados != null && puntajesGuardados.containsKey(nombre)) {
            int puntajeAntiguo = puntajesGuardados.get(nombre);
            nuevoJugador.agregarPuntajeTotal(puntajeAntiguo);
            cliente.enviarMensaje("SISTEMA: Has recuperado tu puntaje anterior: " + puntajeAntiguo);
        }
        // --- FIN LÓGICA DE CARGA ---

        mapaEstadoJugador.put(cliente, nuevoJugador);
        broadcast("LOBBY_UPDATE:Jugadores en sala: " + obtenerListaNombresJugadores());
        
        if (!esAnfitrion(cliente)) {
            cliente.enviarMensaje("Esperando al anfitrión...");
        }
    }
    
    public synchronized void removerJugador(HiloCliente cliente) {
        if (clientesConectados.remove(cliente)) {
            String nombre = cliente.getUsuarioActual();
            mapaNombreCliente.remove(nombre);
            mapaEstadoJugador.remove(cliente);
            broadcast("LOBBY_UPDATE: " + nombre + " ha abandonado la sala.");
        }
    }

    public boolean esAnfitrion(HiloCliente cliente) {
        return !clientesConectados.isEmpty() && clientesConectados.get(0).equals(cliente);
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

    public synchronized void guardarPartida(BaseDeDatos db, HiloCliente solicitante) {
        if (!esAnfitrion(solicitante)) {
            solicitante.enviarMensaje("Error: Solo el anfitrión puede guardar la partida.");
            return;
        }

        List<Jugador> listaJugadores = new ArrayList<>(mapaEstadoJugador.values());
        
        int idGuardado = db.guardarPartida(listaJugadores);

        if (idGuardado != -1) {
            broadcast("SISTEMA: La partida ha sido guardada correctamente con ID: " + idGuardado);
            broadcast("SISTEMA: Los puntajes actuales han quedado registrados.");
        } else {
            solicitante.enviarMensaje("Error: No se pudo guardar la partida en la base de datos.");
        }
    }
    
    private void iniciarNuevaRonda() {
        this.mazo = new Mazo(); 
        this.mazo.barajar();
        this.indiceTurnoActual = 0;

        for (Jugador j : mapaEstadoJugador.values()) {
            j.reiniciarRonda();
        }

        broadcast("------------------------------------------------");
        broadcast("INICIO_RONDA: Ronda #" + numeroRonda);
        broadcast("------------------------------------------------");
        notificarTurno();
    }

    private void notificarTurno() {
        if (verificarFinDeRondaAutomatico()) {
            finalizarRonda(null); 
            return;
        }

        HiloCliente clienteActual = clientesConectados.get(indiceTurnoActual);
        Jugador jugador = mapaEstadoJugador.get(clienteActual);

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
        if (carta.getTipo() == TipoAccion.NUMERO) {
            boolean tieneCarta = false;
            for (Carta c : jugador.getMano()) {
                if (c.getTipo() == TipoAccion.NUMERO && c.getValor() == carta.getValor()) {
                    tieneCarta = true; break;
                }
            }

            if (tieneCarta) {
                if (tieneSecondChance(jugador)) {
                     eliminarSecondChance(jugador);
                     for(int r = 0; r < jugador.getMano().size(); r++) {
                         Carta c = jugador.getMano().get(r);
                         if (c.getTipo() == TipoAccion.NUMERO && c.getValor() == carta.getValor()) {
                             jugador.getMano().remove(r);
                             broadcast("Juego: "+jugador.getNombre() +"descarta su carta coincidente (" + c.getValor() +") como coste de Second Chance.");
                              break;
                         }
                     }
                     jugador.recibirCarta(carta);
                     jugador.calcularPuntajeRonda();
                     cliente.enviarMensaje("ESTADO: Mano: " + jugador.getMano().toString());
                     if(!verificarCondicionesVictoriaRonda(cliente, jugador)) {
                         siguienteTurno(); 
                     }
                } else {
                     broadcast("EXPLOSION: " + jugador.getNombre() + " explotó con un " + carta.getValor());
                     jugador.setPuntajeRonda(0);
                     jugador.getMano().clear();
                     jugador.setEliminadoRonda(true); 
                     siguienteTurno();
                }
            } else {
                jugador.recibirCarta(carta);
                jugador.calcularPuntajeRonda();
                cliente.enviarMensaje("ESTADO: Mano: " + jugador.getMano().toString());
                
                if (!verificarCondicionesVictoriaRonda(cliente, jugador)) {
                    siguienteTurno(); 
                }
            }
            return;
        }

        manejarCartaAccion(cliente, jugador, carta);
    }
    
    private boolean verificarCondicionesVictoriaRonda(HiloCliente cliente, Jugador jugador) {
        if (jugador.getCantidadNumericas() >= 7) {
            broadcast("!!! BONIFICACIÓN !!! " + jugador.getNombre() + " ha juntado 7 cartas numéricas.");
            finalizarRonda(jugador); 
            return true;
        }
        return false;
    }

    private boolean verificarFinDeRondaAutomatico() {
        for (Jugador j : mapaEstadoJugador.values()) {
            if (!j.isPlantado() && !j.isEliminadoRonda()) {
                return false; 
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
            
            j.agregarPuntajeTotal(j.getPuntajeRonda());
            
            resumen.append(j.getNombre())
                   .append(": +").append(j.getPuntajeRonda())
                   .append(" (Total: ").append(j.getPuntajeTotal()).append(")\n");

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
            gestionarFinDeJuego();
        } else {
            numeroRonda++;
            try { Thread.sleep(3000); } catch (InterruptedException e) {}
            iniciarNuevaRonda();
        }
    }

    private void manejarCartaAccion(HiloCliente cliente, Jugador jugador, Carta carta) {
        jugador.recibirCarta(carta); 

        switch (carta.getTipo()) {
            case FREEZE:
                this.accionPendiente = TipoAccion.FREEZE;
                this.jugadorPendienteDeAccion = cliente;
                String candidatosFreeze = obtenerListaCandidatos(cliente.getUsuarioActual(), false);
                cliente.enviarMensaje("SELECCIONAR:FREEZE:¿A quién quieres congelar?:" + candidatosFreeze);
                break;
                
            case FLIP_THREE:
                this.accionPendiente = TipoAccion.FLIP_THREE;
                this.jugadorPendienteDeAccion = cliente;
                String candidatosFlip = obtenerListaCandidatos(cliente.getUsuarioActual(), true);
                cliente.enviarMensaje("SELECCIONAR:FLIP_THREE:¿A quién atacas con 3 cartas?:" + candidatosFlip);
                break;
                
            case SECOND_CHANCE:
                broadcast("ACCION: " + jugador.getNombre() + " obtuvo Second Chance.");
                siguienteTurno(); 
                break;
        }
    }

    public synchronized void procesarSeleccionObjetivo(HiloCliente cliente, String nombreVictima) {
        if (accionPendiente == null || !cliente.equals(jugadorPendienteDeAccion)) {
            cliente.enviarMensaje("Error: No se espera una selección de objetivo o no es tu turno para seleccionar.");
            return;
        }
        
        HiloCliente victima = mapaNombreCliente.get(nombreVictima);
        if (victima == null) {
            cliente.enviarMensaje("Error: El jugador seleccionado no existe o no está en la sala.");
            return;
        }

        TipoAccion accionAResolver = accionPendiente; 
        accionPendiente = null;
        jugadorPendienteDeAccion = null;
        
        // CORRECCIÓN: Obtenemos el jugador para borrar la carta usada
        Jugador atacante = mapaEstadoJugador.get(cliente);

        if (accionAResolver == TipoAccion.FREEZE) {
            if (nombreVictima.equals(cliente.getUsuarioActual())) {
                cliente.enviarMensaje("Error: No puedes congelarte a ti mismo.");
                siguienteTurno();
                return; 
            }
            
            // CORRECCIÓN: Borramos la carta FREEZE de la mano tras usarla
            eliminarCartaDeMano(atacante, TipoAccion.FREEZE);

            Jugador jugVictima = mapaEstadoJugador.get(victima);
            broadcast("FREEZE: " + cliente.getUsuarioActual() + " congela a " + nombreVictima);
            
            jugVictima.setPlantado(true); 
            broadcast("EFECTO: " + nombreVictima + " ha quedado congelado (No puede robar más en esta ronda).");
            siguienteTurno(); 

        } else if (accionAResolver == TipoAccion.FLIP_THREE) {
            // CORRECCIÓN: Borramos la carta FLIP_THREE de la mano tras usarla
            eliminarCartaDeMano(atacante, TipoAccion.FLIP_THREE);

            broadcast("FLIP_THREE: " + cliente.getUsuarioActual() + " ataca a " + nombreVictima);
            aplicarFlipThree(cliente, nombreVictima);
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
            
            if (jugVictima.isEliminadoRonda()) break; 
        }
        siguienteTurno();
    }
    
    private void procesarCartaForzada(HiloCliente cliente, Jugador jugador, Carta carta) {
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

    private void gestionarFinDeJuego() {
        this.jugadoresListosParaReiniciar = new HashMap<>();

        broadcast("FIN_JUEGO_VOTO: La partida ha terminado. ¿Desean jugar de nuevo o salir? (REINICIAR / VOTAR_SALIR)");
        broadcast("Tienes 15 segundos para responder. Tiempo límite: " + TIEMPO_ESPERA_FIN_JUEGO / 1000 + "s");

        Thread timerThread = new Thread(() -> {
            try {
                Thread.sleep(TIEMPO_ESPERA_FIN_JUEGO);
                
                synchronized (this) {
                    terminarVotacion();
                }
            } catch (InterruptedException e) {
            }
        });
        timerThread.start();
    }

    public synchronized void procesarVoto(HiloCliente cliente, String comando) {
        if (juegoIniciado) return;

        if (jugadoresListosParaReiniciar != null && jugadoresListosParaReiniciar.containsKey(cliente)) {
            cliente.enviarMensaje("Error: Ya has votado.");
            return;
        }
        
        if (comando.equals("REINICIAR")) {
            if(jugadoresListosParaReiniciar == null) jugadoresListosParaReiniciar = new HashMap<>();
            jugadoresListosParaReiniciar.put(cliente, true);
            broadcast("VOTO: " + cliente.getUsuarioActual() + " ha votado para REINICIAR.");
        } else if (comando.equals("SALIR_SALA")) {
            removerJugador(cliente);
            cliente.enviarMensaje("SALIDA_EXITOSA: Has salido de la sala.");
        }

        if (jugadoresListosParaReiniciar != null && clientesConectados.size() == jugadoresListosParaReiniciar.size()) {
            terminarVotacion();
        }
    }

    private synchronized void terminarVotacion() {
        List<HiloCliente> jugadoresARemover = new ArrayList<>();
        
        for (HiloCliente cliente : clientesConectados) {
            if (jugadoresListosParaReiniciar == null || !jugadoresListosParaReiniciar.containsKey(cliente)) {
                jugadoresARemover.add(cliente);
                cliente.enviarMensaje("SALIDA_FORZADA: No respondiste a tiempo o votaste salir. Fuiste sacado de la sala.");
            }
        }

        for (HiloCliente cliente : jugadoresARemover) {
            removerJugador(cliente);
        }

        if (clientesConectados.size() >= 2) {
            broadcast("\n=== Reiniciando Partida ===");
            this.juegoIniciado = true; 
            iniciarNuevaRonda(); 
        } else {
            broadcast("\n=== Votación Finalizada ===");
            if (!clientesConectados.isEmpty()) {
                 broadcast("No hay suficientes jugadores (mínimo 2) para reiniciar. Esperando nuevos jugadores...");
            }
        }
        this.jugadoresListosParaReiniciar = null; 
    }

    // --- MÉTODOS AUXILIARES ---

    // NUEVO: Método auxiliar para borrar la carta usada de la mano
    private void eliminarCartaDeMano(Jugador j, TipoAccion tipo) {
        for(int i = 0; i < j.getMano().size(); i++) {
            if(j.getMano().get(i).getTipo() == tipo) {
                j.getMano().remove(i);
                return; // Solo borramos UNA instancia de la carta
            }
        }
    }

    private String obtenerListaNombresJugadores() { 
        StringBuilder sb = new StringBuilder();
        for(String s : mapaNombreCliente.keySet()) sb.append(s).append(",");
        return sb.toString();
    }
    private boolean tieneSecondChance(Jugador j) { 
        for(Carta c : j.getMano()) if(c.getTipo() == TipoAccion.SECOND_CHANCE) return true;
        return false;
    }
    private void eliminarSecondChance(Jugador j) {
         for(int i=0; i<j.getMano().size(); i++){
             if(j.getMano().get(i).getTipo() == TipoAccion.SECOND_CHANCE){ j.getMano().remove(i); return; }
         }
    }
    private String obtenerListaCandidatos(String miNombre, boolean incluirme) {
        StringBuilder sb = new StringBuilder();
        for(String n : mapaNombreCliente.keySet()){ if(!incluirme && n.equals(miNombre)) continue; sb.append(n).append(","); }
        return sb.toString();
    }
    public void broadcast(String mensaje) {
        for (HiloCliente c : clientesConectados) {
            c.enviarMensaje(mensaje); 
        }
    }
    public boolean isJuegoIniciado() {
        return juegoIniciado;
    }
}