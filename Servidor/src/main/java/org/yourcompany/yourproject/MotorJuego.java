package org.yourcompany.yourproject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.yourcompany.yourproject.Cartas.src.main.java.Mazo.*;

public class MotorJuego {

    // --- Referencias ---
    private final NotificacionJuego notificador;
    
    // --- Estado del Juego ---
    private List<Jugador> jugadores; 
    private Map<String, Jugador> mapaJugadores; 
    
    private Mazo mazo;
    private boolean juegoIniciado;
    private int indiceTurnoActual;
    private int numeroRonda;

    // --- Estados de Acciones Especiales ---
    private TipoAccion accionPendiente = null;
    private String nombreJugadorPendiente = null; 
    
    // --- Variables Flip Three ---
    private boolean enModoFlipThree = false;
    private int cartasRestantesFlipThree = 0;
    private String nombreVictimaFlipThree = null; 

    public MotorJuego(NotificacionJuego notificador) {
        this.notificador = notificador;
        this.jugadores = new ArrayList<>();
        this.mapaJugadores = new HashMap<>();
        this.numeroRonda = 1;
        this.juegoIniciado = false;
    }

    public boolean isJuegoIniciado() { return juegoIniciado; }
    public List<Jugador> getJugadores() { return jugadores; }
    public boolean isEnModoFlipThree() { return enModoFlipThree; } // Necesario para las cartas

    // --- Gestión de Jugadores ---
    public synchronized void agregarJugador(String nombre) {
        if (juegoIniciado) return;
        Jugador j = new Jugador(nombre);
        jugadores.add(j);
        mapaJugadores.put(nombre, j);
    }
    
    public synchronized void removerJugador(String nombre) {
        Jugador j = mapaJugadores.remove(nombre);
        jugadores.remove(j);
    }

    public void cargarPuntajesPrevios(Map<String, Integer> puntajes) {
        for (Jugador j : jugadores) {
            if (puntajes.containsKey(j.getNombre())) {
                j.agregarPuntajeTotal(puntajes.get(j.getNombre()));
                notificador.enviarMensajePrivado(j.getNombre(), "SISTEMA: Puntaje recuperado: " + puntajes.get(j.getNombre()));
            }
        }
    }

    // --- Inicio del Juego ---
    public synchronized void iniciarJuego() {
        if (jugadores.size() < 2) {
            notificador.broadcast("Error: Se necesitan al menos 2 jugadores.");
            return;
        }
        this.juegoIniciado = true;
        this.numeroRonda = 1;
        notificador.broadcast("JUEGO_INICIADO");
        iniciarNuevaRonda();
    }

    private void iniciarNuevaRonda() {
        this.mazo = new Mazo();
        this.mazo.barajar();
        this.indiceTurnoActual = 0;
        this.enModoFlipThree = false;
        
        for (Jugador j : jugadores) j.reiniciarRonda();

        notificador.broadcast("------------------------------------------------");
        notificador.broadcast("INICIO_RONDA: Ronda #" + numeroRonda);
        notificador.broadcast("------------------------------------------------");
        notificarTurno();
    }

    // --- Lógica de Turnos ---
    public void notificarTurno() { 
        if (verificarFinDeRondaAutomatico()) {
            finalizarRonda(null);
            return;
        }
        
        Jugador actual = jugadores.get(indiceTurnoActual);
        
        if (actual.isPlantado() || actual.isEliminadoRonda()) {
            avanzarIndiceTurno();
            notificarTurno(); 
            return;
        }

        notificador.broadcast("TURNO: Es el turno de " + actual.getNombre());
        notificador.enviarMensajePrivado(actual.getNombre(), "TU_TURNO: ¿Qué deseas hacer? (ROBAR / PLANTARSE)");
    }
    
    private void avanzarIndiceTurno() {
        indiceTurnoActual = (indiceTurnoActual + 1) % jugadores.size();
    }

    public void avanzarTurno() { 
        avanzarIndiceTurno();
        notificarTurno();
    }

    // --- PROCESAMIENTO PRINCIPAL ---
    public synchronized void procesarAccion(String nombreJugador, String accion) {
        Jugador actual = jugadores.get(indiceTurnoActual);
        
        if (!actual.getNombre().equals(nombreJugador)) {
            notificador.enviarMensajePrivado(nombreJugador, "Error: No es tu turno.");
            return;
        }
        if (accionPendiente != null) {
            notificador.enviarMensajePrivado(nombreJugador, "Error: Tienes una acción pendiente (" + accionPendiente + ")");
            return;
        }

        if (accion.equals("PLANTARSE")) {
            actual.setPlantado(true);
            notificador.broadcast("JUEGO: " + actual.getNombre() + " se planta con " + actual.getPuntajeRonda() + " pts.");
            avanzarTurno();
        } else if (accion.equals("ROBAR")) {
            ejecutarRobar(actual);
        } else {
            notificador.enviarMensajePrivado(nombreJugador, "Error: Acción desconocida.");
        }
    }

    private void ejecutarRobar(Jugador jugador) {
        Carta carta = mazo.tomarCarta();
        if (carta == null) {
            notificador.broadcast("JUEGO: Se acabó el mazo.");
            finalizarRonda(null);
            return;
        }
        notificador.broadcast("JUEGO: " + jugador.getNombre() + " sacó " + carta.toString());
        jugador.recibirCarta(carta); 

        carta.activar(this, jugador);
    }

    // --- API PARA LAS CARTAS (MÉTODOS PÚBLICOS QUE LAS CARTAS LLAMAN) ---

    /**
     * Llamado por CartaNumerica
     */
    public void procesarNumero(Jugador jugador, Carta carta) {
        boolean exploto = verificarExplosion(jugador, carta);
        if (!exploto) {
            jugador.calcularPuntajeRonda();
            // Verificar si ganó por tener 7 cartas numéricas
            if (!verificarCondicionesVictoriaRonda(jugador)) {
                // Si no estamos en medio de un ataque Flip3, pasamos turno
                if (!enModoFlipThree) {
                    avanzarTurno();
                }
            }
        }
    }

    /**
     * Llamado por CartaFreeze y CartaFlipThree
     */
    public void solicitarSeleccion(Jugador jugador, TipoAccion tipo, String mensaje) {
        this.accionPendiente = tipo;
        this.nombreJugadorPendiente = jugador.getNombre();
        notificador.enviarMensajePrivado(jugador.getNombre(), "SELECCIONAR:" + tipo + ":" + mensaje);
    }

    /**
     * Llamado por CartaBono (Puntos, Multiplicador, Second Chance)
     */
    public void notificarBono(Jugador jugador, String mensaje) {
        notificador.broadcast("BONUS: " + mensaje);
        if (!enModoFlipThree) {
            avanzarTurno();
        }
    }

    // --- Lógica Específica de Reglas ---

    private boolean verificarExplosion(Jugador jugador, Carta carta) {
        // Solo verificamos explosión si la carta nueva es un número
        if (carta.getTipo() != TipoAccion.NUMERO) return false;

        boolean duplicado = false;
        // Buscar duplicados en la mano (excluyendo la carta recién agregada si ya la añadiste antes)
        int contador = 0;
        for (Carta c : jugador.getMano()) {
            if (c.getTipo() == TipoAccion.NUMERO && c.getValor() == carta.getValor()) {
                contador++;
            }
        }
        if (contador > 1) duplicado = true; // Si hay más de 1 (la vieja y la nueva)

        if (duplicado) {
            if (tieneSecondChance(jugador)) {
                eliminarSecondChance(jugador);
                // Eliminar la carta vieja conflictiva
                eliminarUnaCartaPorValor(jugador, carta.getValor());
                
                jugador.calcularPuntajeRonda();
                notificador.broadcast(">> " + jugador.getNombre() + " se salvó usando Second Chance!");
                broadcastEstadoMesa();
                return false; 
            } else {
                notificador.broadcast("EXPLOSION: " + jugador.getNombre() + " explotó con el par de " + carta.getValor());
                jugador.setPuntajeRonda(0);
                jugador.getMano().clear();
                jugador.setEliminadoRonda(true);
                
                if (enModoFlipThree) {
                    // Si explotó durante Flip Three, termina el ataque
                    enModoFlipThree = false;
                    avanzarTurno();
                } else {
                    avanzarTurno();
                }
                return true; 
            }
        } else {
            jugador.calcularPuntajeRonda();
            broadcastEstadoMesa();
            return false;
        }
    }

    // --- Respuesta del Usuario (Target Selection) ---
    
    public synchronized void procesarSeleccionObjetivo(String nombreAtacante, String nombreVictima) {
        if (accionPendiente == null || !nombreAtacante.equals(nombreJugadorPendiente)) {
            notificador.enviarMensajePrivado(nombreAtacante, "Error: No se espera selección.");
            return;
        }

        Jugador victima = mapaJugadores.get(nombreVictima);
        if (victima == null) {
            notificador.enviarMensajePrivado(nombreAtacante, "Error: Jugador no encontrado.");
            return;
        }

        Jugador atacante = mapaJugadores.get(nombreAtacante);
        TipoAccion accion = accionPendiente;
        
        // Limpiamos estado pendiente
        accionPendiente = null;
        nombreJugadorPendiente = null;

        // Ejecutar efecto
        if (accion == TipoAccion.FREEZE) {
            if (nombreAtacante.equals(nombreVictima)) {
                notificador.enviarMensajePrivado(nombreAtacante, "No puedes congelarte a ti mismo (pierdes el turno).");
                avanzarTurno();
                return;
            }
            eliminarCartaDeMano(atacante, TipoAccion.FREEZE);
            victima.setPlantado(true);
            notificador.broadcast("FREEZE: " + nombreAtacante + " congeló a " + nombreVictima);
            
            if(enModoFlipThree) continuarFlipThree();
            else avanzarTurno();

        } else if (accion == TipoAccion.FLIP_THREE) {
            eliminarCartaDeMano(atacante, TipoAccion.FLIP_THREE);
            notificador.broadcast("FLIP_THREE: " + nombreAtacante + " ataca a " + nombreVictima);
            iniciarFlipThree(nombreVictima);
        }
    }

    // --- Lógica Flip Three ---
    private void iniciarFlipThree(String nombreVictima) {
        this.enModoFlipThree = true;
        this.nombreVictimaFlipThree = nombreVictima;
        this.cartasRestantesFlipThree = 3;
        continuarFlipThree();
    }

    private void continuarFlipThree() {
        Jugador victima = mapaJugadores.get(nombreVictimaFlipThree);
        
        if (cartasRestantesFlipThree <= 0 || victima.isEliminadoRonda()) {
            enModoFlipThree = false;
            nombreVictimaFlipThree = null;
            avanzarTurno();
            return;
        }

        cartasRestantesFlipThree--;
        
        // Simulación de pausa
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        Carta c = mazo.tomarCarta();
        if (c == null) {
            notificador.broadcast("Mazo vacío en ataque.");
            finalizarRonda(null);
            return;
        }
        
        notificador.broadcast("FLIP_THREE: " + victima.getNombre() + " voltea... " + c.toString());
        
        // En Flip Three, la lógica es especial:
        // Si es número -> Se verifica explosión.
        // Si es acción -> Se guarda en la mano pero NO se activa su efecto inmediatamente.
        
        jugadorRecibeEnFlipThree(victima, c);
    }

    private void jugadorRecibeEnFlipThree(Jugador victima, Carta c) {
        victima.recibirCarta(c);
        
        if (c.getTipo() == TipoAccion.NUMERO) {
            // Si es número, reutilizamos la lógica de explosión
            boolean exploto = verificarExplosion(victima, c);
            if (!exploto) {
                continuarFlipThree();
            }
            // Si explotó, verificarExplosion ya manejó el cambio de turno
        } else {
            // Si es especial (Bono, Freeze, etc), en Flip 3 NO se activa, solo se guarda.
            notificador.enviarMensajePrivado(victima.getNombre(), "Obtuviste " + c.getTipo() + " (guardada).");
            broadcastEstadoMesa();
            continuarFlipThree();
        }
    }

    // --- Finalización y Utilidades ---
    private boolean verificarCondicionesVictoriaRonda(Jugador jugador) {
        if (jugador.getCantidadNumericas() >= 7) {
            notificador.broadcast("!!! BONIFICACIÓN !!! " + jugador.getNombre() + " juntó 7 números.");
            finalizarRonda(jugador);
            return true;
        }
        return false;
    }

    private boolean verificarFinDeRondaAutomatico() {
        for (Jugador j : jugadores) {
            if (!j.isPlantado() && !j.isEliminadoRonda()) return false;
        }
        return true;
    }

    private void finalizarRonda(Jugador ganadorAutomatico) {
        notificador.broadcast("\n=== FIN DE LA RONDA " + numeroRonda + " ===");
        enModoFlipThree = false;
        
        StringBuilder resumen = new StringBuilder("Puntajes Ronda:\n");
        boolean hayGanadorJuego = false;
        String nombreGanador = "";

        for (Jugador j : jugadores) {
            j.agregarPuntajeTotal(j.getPuntajeRonda());
            resumen.append(j.getNombre()).append(": +").append(j.getPuntajeRonda())
                   .append(" (Total: ").append(j.getPuntajeTotal()).append(")\n");
            
            if (j.getPuntajeTotal() >= 200) {
                hayGanadorJuego = true;
                nombreGanador = j.getNombre();
            }
        }
        notificador.broadcast(resumen.toString());

        if (hayGanadorJuego) {
            notificador.onFinDeJuego(nombreGanador);
            juegoIniciado = false;
        } else {
            numeroRonda++;
            try { Thread.sleep(3000); } catch (InterruptedException e) {}
            iniciarNuevaRonda();
        }
    }
    public void notificarTodos(String mensaje) {
        notificador.broadcast(mensaje);
    }
    
    // Auxiliares
    private boolean tieneSecondChance(Jugador j) {
        for(Carta c : j.getMano()) if(c.getTipo() == TipoAccion.SECOND_CHANCE) return true;
        return false;
    }
    private void eliminarSecondChance(Jugador j) {
         eliminarCartaDeMano(j, TipoAccion.SECOND_CHANCE);
    }
    private void eliminarCartaDeMano(Jugador j, TipoAccion tipo) {
        for(int i=0; i<j.getMano().size(); i++){
            if(j.getMano().get(i).getTipo() == tipo){ j.getMano().remove(i); return; }
        }
    }
    private void eliminarUnaCartaPorValor(Jugador j, int valor) {
        for(int i=0; i<j.getMano().size(); i++){
            // Buscamos una carta que sea NUMERO y tenga ese valor
            Carta c = j.getMano().get(i);
            if(c.getTipo() == TipoAccion.NUMERO && c.getValor() == valor){ 
                j.getMano().remove(i); 
                return; 
            }
        }
    }
    private void broadcastEstadoMesa() {
        StringBuilder sb = new StringBuilder("\n=== ESTADO DE LA MESA ===\n");
        for (Jugador j : jugadores) {
            sb.append(String.format("%-10s", j.getNombre())).append(": ").append(j.getMano().toString());
            if (j.isPlantado()) sb.append(" [PLANTADO]");
            else if (j.isEliminadoRonda()) sb.append(" [ELIMINADO]");
            sb.append("\n");
        }
        sb.append("=========================");
        notificador.broadcast(sb.toString());
    }
}