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
    
    private Map<String, Integer> puntajesGuardados = null; 

    private TipoAccion accionPendiente = null; 
    private HiloCliente jugadorPendienteDeAccion = null;
    
    private final long TIEMPO_ESPERA_FIN_JUEGO = 15000; 
    private Map<HiloCliente, Boolean> jugadoresListosParaReiniciar; 

    private Mazo mazo;
    private boolean juegoIniciado;
    private int indiceTurnoActual;
    private int numeroRonda = 1; 

    // Variables para controlar el FLIP_THREE recursivo
    private boolean enModoFlipThree = false;
    private int cartasRestantesFlipThree = 0;
    private HiloCliente victimaFlipThree = null;

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

    public void establecerDatosCargados(Map<String, Integer> datos) {
        this.puntajesGuardados = datos;
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
        
        if (puntajesGuardados != null && puntajesGuardados.containsKey(nombre)) {
            int puntajeAntiguo = puntajesGuardados.get(nombre);
            nuevoJugador.agregarPuntajeTotal(puntajeAntiguo);
            cliente.enviarMensaje("SISTEMA: Has recuperado tu puntaje anterior: " + puntajeAntiguo);
        }

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
        } else {
            solicitante.enviarMensaje("Error: No se pudo guardar la partida.");
        }
    }

    
    
    private void iniciarNuevaRonda() {
        this.mazo = new Mazo(); 
        this.mazo.barajar();
        this.indiceTurnoActual = 0;
        this.enModoFlipThree = false; 

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
            cliente.enviarMensaje("Error: Tienes una acción (" + accionPendiente.toString() + ") pendiente.");
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
            cliente.enviarMensaje("Error: Acción no válida.");
        }
    }

private void procesarCartaSacada(HiloCliente cliente, Jugador jugador, Carta carta) {
    // CASO 1: Es un número
    if (carta.getTipo() == TipoAccion.NUMERO) {
        // Delegamos toda la lógica al método que acabamos de arreglar
        boolean exploto = verificarExplosion(cliente, jugador, carta);
        
        // Si NO explotó y NO estamos en modo Flip Three (que aquí nunca deberíamos estar, pero por seguridad)
        if (!exploto) {
            if (!verificarCondicionesVictoriaRonda(cliente, jugador)) {
                siguienteTurno();
            }
        }
        return;
    }

    // CASO 2: Es una carta especial
    manejarCartaAccion(cliente, jugador, carta);
}
    
  private boolean verificarExplosion(HiloCliente cliente, Jugador jugador, Carta carta) {
    boolean tieneCarta = false;

    // 1. Buscamos si ya tiene el número (ESTO FALTABA)
    for (Carta c : jugador.getMano()) {
        if (c.getTipo() == TipoAccion.NUMERO && c.getValor() == carta.getValor()) {
            tieneCarta = true; 
            break;
        }
    }
    

    if (tieneCarta) {
        if (tieneSecondChance(jugador)) {
             eliminarSecondChance(jugador);
             // Eliminar la carta que causaba conflicto de la mano (regla general)
             for(int r = 0; r < jugador.getMano().size(); r++) {
                 Carta c = jugador.getMano().get(r);
                 if (c.getTipo() == TipoAccion.NUMERO && c.getValor() == carta.getValor()) {
                     jugador.getMano().remove(r);
                     break;
                 }
             }
             
             jugador.recibirCarta(carta);
             jugador.calcularPuntajeRonda();
             
             broadcast(">> " + jugador.getNombre() + " se salvó usando Second Chance!");
             broadcastEstadoMesa(); 

             // Si NO estamos en Flip Three, verificamos si ganó o pasamos turno
             if(!enModoFlipThree) {
                 if(!verificarCondicionesVictoriaRonda(cliente, jugador)) siguienteTurno();
             }
             return false; // No explotó (se salvó)
        } else {
             broadcast("EXPLOSION: " + jugador.getNombre() + " explotó con un " + carta.getValor());
             jugador.setPuntajeRonda(0);
             jugador.getMano().clear();
             jugador.setEliminadoRonda(true); 
            
             if(!enModoFlipThree) siguienteTurno();
             return true; // Sí explotó
        }
    } else {
        // No tiene la carta, todo bien
        jugador.recibirCarta(carta);
        jugador.calcularPuntajeRonda();
        broadcastEstadoMesa();
        return false; // No explotó
    }
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
        enModoFlipThree = false; // Resetear
        
        StringBuilder resumen = new StringBuilder("Puntajes Ronda:\n");
        boolean hayGanadorJuego = false;
        String nombreGanador = "";

        for (HiloCliente hc : clientesConectados) {
            Jugador j = mapaEstadoJugador.get(hc);
            j.agregarPuntajeTotal(j.getPuntajeRonda());
            resumen.append(j.getNombre()).append(": +").append(j.getPuntajeRonda())
                   .append(" (Total: ").append(j.getPuntajeTotal()).append(")\n");

            if (j.getPuntajeTotal() >= 200) {
                hayGanadorJuego = true;
                nombreGanador = j.getNombre();
            }
        }
        broadcast(resumen.toString());

        if (hayGanadorJuego) {
            broadcast("!!! FIN DEL JUEGO !!! GANADOR: " + nombreGanador);
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
        
        // Calculamos puntaje inmediatamente para actualizar la vista
        jugador.calcularPuntajeRonda(); 

        switch (carta.getTipo()) {
            case FREEZE:
                break;
                
            case FLIP_THREE:
                break;
                
            case SECOND_CHANCE:
                broadcast("ACCION: " + jugador.getNombre() + " obtuvo Second Chance.");
                if (!enModoFlipThree) siguienteTurno();
                break;

            case PUNTOS:
                broadcast("BONUS: " + jugador.getNombre() + " obtuvo +" + carta.getValor() + " puntos!");
                if (!enModoFlipThree) siguienteTurno();
                break;

            case MULTIPLICADOR:
                broadcast("BONUS: " + jugador.getNombre() + " obtuvo un MULTIPLICADOR x" + carta.getValor() + "!");
                if (!enModoFlipThree) siguienteTurno();
                break;
        }
    }

    public synchronized void procesarSeleccionObjetivo(HiloCliente cliente, String nombreVictima) {
        if (accionPendiente == null || !cliente.equals(jugadorPendienteDeAccion)) {
            cliente.enviarMensaje("Error: No se espera una selección de objetivo.");
            return;
        }
        
        HiloCliente victima = mapaNombreCliente.get(nombreVictima);
        if (victima == null) {
            cliente.enviarMensaje("Error: El jugador seleccionado no existe.");
            return;
        }

        TipoAccion accionAResolver = accionPendiente; 
        accionPendiente = null;
        jugadorPendienteDeAccion = null;
        
        Jugador atacante = mapaEstadoJugador.get(cliente);

        if (accionAResolver == TipoAccion.FREEZE) {
            if (nombreVictima.equals(cliente.getUsuarioActual())) {
                cliente.enviarMensaje("Error: No puedes congelarte a ti mismo.");
                if(!enModoFlipThree) siguienteTurno();
                else continuarFlipThree();
                return; 
            }
            eliminarCartaDeMano(atacante, TipoAccion.FREEZE);
            Jugador jugVictima = mapaEstadoJugador.get(victima);
            broadcast("FREEZE: " + cliente.getUsuarioActual() + " congela a " + nombreVictima);
            jugVictima.setPlantado(true); 
            broadcast("EFECTO: " + nombreVictima + " ha quedado congelado.");
            
            if (enModoFlipThree) continuarFlipThree();
            else siguienteTurno(); 

        } else if (accionAResolver == TipoAccion.FLIP_THREE) {
            eliminarCartaDeMano(atacante, TipoAccion.FLIP_THREE);
            broadcast("FLIP_THREE: " + cliente.getUsuarioActual() + " ataca a " + nombreVictima);
            
            aplicarFlipThree(cliente, nombreVictima);
        }
    }
    

    // --- LÓGICA CORREGIDA: FLIP THREE SIN PAUSA POR ACCIONES ---

    public synchronized void aplicarFlipThree(HiloCliente atacante, String nombreVictima) {
        HiloCliente clienteVictima = mapaNombreCliente.get(nombreVictima);
        
        this.enModoFlipThree = true;
        this.victimaFlipThree = clienteVictima;
        this.cartasRestantesFlipThree = 3;

        continuarFlipThree();
    }

    private void continuarFlipThree() {
        Jugador jugVictima = mapaEstadoJugador.get(victimaFlipThree);
        
        // Condición de parada
        if (cartasRestantesFlipThree <= 0 || jugVictima.isEliminadoRonda()) {
            this.enModoFlipThree = false;
            this.victimaFlipThree = null;
            siguienteTurno(); 
            return;
        }

        // Descontamos y robamos
        cartasRestantesFlipThree--;
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        
        Carta c = mazo.tomarCarta();
        if (c == null) {
            broadcast("JUEGO: Se acabó el mazo durante el ataque.");
            finalizarRonda(null);
            return;
        }

        broadcast("FLIP_THREE: " + victimaFlipThree.getUsuarioActual() + " voltea... " + c.toString());

        // 1. Si es NUMERO: Verificamos explosión normalmente
        if (c.getTipo() == TipoAccion.NUMERO) {
            boolean exploto = verificarExplosion(victimaFlipThree, jugVictima, c);
            if (exploto) {
                this.enModoFlipThree = false;
                siguienteTurno();
            } else {
                // Se salvó o no explotó, continuamos
                continuarFlipThree();
            }
        } 
        // 2. Si es CARTA DE ACCIÓN (Freeze, Flip, Second Chance, etc.)
        else {
            // CORRECCIÓN: NO se juega la carta. Se la guarda y se sigue robando.
            jugVictima.recibirCarta(c);
            
            // Notificamos discretamente
            String nombreCarta = c.getTipo().toString();
            victimaFlipThree.enviarMensaje("SISTEMA: Obtuviste " + nombreCarta + " durante el ataque. Se guarda en tu mano.");

            broadcastEstadoMesa();
            
            // SEGUIMOS ROBANDO INMEDIATAMENTE
            continuarFlipThree();
        }
    }

    // ------------------------------------------------

    private void gestionarFinDeJuego() {
        this.jugadoresListosParaReiniciar = new HashMap<>();

        broadcast("FIN_JUEGO_VOTO: La partida ha terminado. (REINICIAR / VOTAR_SALIR)");
        
        Thread timerThread = new Thread(() -> {
            try {
                Thread.sleep(TIEMPO_ESPERA_FIN_JUEGO);
                synchronized (this) { terminarVotacion(); }
            } catch (InterruptedException e) {}
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
            broadcast("VOTO: " + cliente.getUsuarioActual() + " quiere REINICIAR.");
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
                cliente.enviarMensaje("SALIDA_FORZADA: No votaste reiniciar.");
            }
        }
        for (HiloCliente cliente : jugadoresARemover) removerJugador(cliente);

        if (clientesConectados.size() >= 2) {
            broadcast("\n=== Reiniciando Partida ===");
            this.juegoIniciado = true; 
            iniciarNuevaRonda(); 
        } else {
            broadcast("\n=== Votación Finalizada ===");
        }
        this.jugadoresListosParaReiniciar = null; 
    }

    // --- MÉTODOS AUXILIARES ---
    private void eliminarCartaDeMano(Jugador j, TipoAccion tipo) {
        for(int i = 0; i < j.getMano().size(); i++) {
            if(j.getMano().get(i).getTipo() == tipo) {
                j.getMano().remove(i);
                return; 
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

    private void broadcastEstadoMesa() {
    StringBuilder sb = new StringBuilder("\n=== ESTADO DE LA MESA ===\n");
    
    for (HiloCliente hc : clientesConectados) {
        Jugador j = mapaEstadoJugador.get(hc);
        
        sb.append(String.format("%-10s", j.getNombre())) // Nombre alineado
          .append(": ").append(j.getMano().toString());  // Sus cartas
        
        // Agregamos info extra útil
        if (j.isPlantado()) {
            sb.append(" [PLANTADO - ").append(j.getPuntajeRonda()).append(" pts]");
        } else if (j.isEliminadoRonda()) {
            sb.append(" [ELIMINADO]");
        } else {
            sb.append(" (Jugando...)");
        }
        sb.append("\n");
    }
    sb.append("=========================");
    
    // Enviamos este resumen a TODOS los conectados
    broadcast(sb.toString());
}


}