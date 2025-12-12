package org.yourcompany.yourproject;

import org.yourcompany.yourproject.Cartas.src.main.java.Mazo.Jugador;

public class ProcesadorMensajes {

    private final HiloCliente cliente;
    private final BaseDeDatos db;

    public ProcesadorMensajes(HiloCliente cliente, BaseDeDatos db) {
        this.cliente = cliente;
        this.db = db;
    }

    public void procesar(String mensaje) {
        String[] partes = mensaje.split(":");
        String comando = partes[0];

        switch (comando) {
            case "REGISTRAR":
                handleRegistro(partes);
                break;

            case "LOGIN":
                handleLogin(partes);
                break;
            
            case "CREAR_SALA":
                handleCrearSala();
                break;

            case "UNIRSE_SALA":
                handleUnirseSala(partes);
                break;
         
            case "REINICIAR":
            case "VOTAR_SALIR":
                handleVotoFinJuego(comando);
                break;    

            case "SALIR_SALA":
                handleSalirSala();
                break;

            case "SALIR":
                cliente.enviarMensaje("Cerrando conexión. ¡Adiós!");
                cliente.cerrarConexion();
                break;

            case "INVITAR": 
                handleInvitar(partes);
                break;

            case "ACEPTAR_INVITACION": 
                handleUnirseSala(partes); 
                break;

            case "INICIAR_PARTIDA":
                handleIniciarPartida();
                break;

            case "SELECCIONAR_OBJETIVO": 
                handleSeleccionObjetivo(partes);
                break;

            case "ROBAR":
            case "PLANTARSE":
                handleJugada(comando); // <--- Nuevo método que crearemos abajo
                break;
            
            case "GUARDAR":
                handleGuardarPartida();
                break;
                
            // NUEVO: Caso para cargar la partida
            case "CARGAR_PARTIDA":
                handleCargarPartida(partes);
                break;

            default:
                cliente.enviarMensaje("Comando no reconocido.");
                break;
        }
    }

    private void handleRegistro(String[] partes) {
        if (partes.length < 3) {
            cliente.enviarMensaje("Error: Faltan datos para registrar.");
            return;
        }

        if (db.registrarUsuario(partes[1], partes[2])) {
            cliente.enviarMensaje("Registro exitoso.");
        } else {
            cliente.enviarMensaje("Registro fallido: Usuario ya existe.");
        }
    }

    private void handleLogin(String[] partes) {
        if (partes.length < 3) {
            cliente.enviarMensaje("Error: Faltan datos para login.");
            return;
        }
        String usuario = partes[1];
        String password = partes[2];

        if (db.validarUsuario(usuario, password)) {
            if (org.yourcompany.yourproject.Servidor.clientesOnline.containsKey(usuario)) {
                cliente.enviarMensaje("Error: La cuenta '" + usuario + "' ya está conectada desde otro lugar.");
                return; 
            }
            cliente.setUsuarioActual(usuario); 
            Jugador nuevoJugador = new Jugador(usuario); 
            cliente.enviarMensaje("Sesion iniciada. bienvenido: " + nuevoJugador.getNombre());
        } else {
            cliente.enviarMensaje("Error: Usuario/Contraseña incorrectas.");
        }
    }
    
    private void handleCrearSala() {
        if (cliente.getUsuarioActual() == null) {
            cliente.enviarMensaje("Error: Debes iniciar sesión primero.");
            return;
        }

        Sala salaActual = GestorSalas.buscarSalaDeJugador(cliente);
        if (salaActual != null) {
            cliente.enviarMensaje("Error: Ya estás en la sala " + salaActual.getId() + ". Debes salir primero.");
            return; 
        }

        int idSala = GestorSalas.crearSala(cliente);
        cliente.enviarMensaje("Sala creada con éxito. ID: " + idSala);
        cliente.enviarMensaje("Esperando jugadores... (Envía 'INICIAR_PARTIDA' para comenzar)");
    }

    private void handleUnirseSala(String[] partes) {
        if (cliente.getUsuarioActual() == null) {
            cliente.enviarMensaje("Error: Debes iniciar sesión primero.");
            return;
        }

        Sala salaActual = GestorSalas.buscarSalaDeJugador(cliente);
        if (salaActual != null) {
            cliente.enviarMensaje("Error: Ya estás en la sala " + salaActual.getId() + ". Debes salir primero.");
            return;
        }

        if (partes.length < 2) {
            cliente.enviarMensaje("Error: Debes especificar el ID de la sala.");
            return;
        }
        
        try {
            int idSala = Integer.parseInt(partes[1]);
            Sala sala = GestorSalas.obtenerSala(idSala);
            if (sala != null) {
                sala.agregarJugador(cliente);
            } else {
                cliente.enviarMensaje("Error: Sala no encontrada.");
            }
        } catch (NumberFormatException e) {
            cliente.enviarMensaje("Error: ID de sala inválido.");
        }
    }

    private void handleInvitar(String[] partes) {
        if (partes.length < 2) return;
        String nombreInvitado = partes[1];
        String anfitrion = cliente.getUsuarioActual();
        
        HiloCliente invitado = Servidor.clientesOnline.get(nombreInvitado);
        
        if (invitado != null) {
            Sala sala = GestorSalas.buscarSalaDeJugador(cliente); 
            if (sala != null) {
                invitado.enviarMensaje("INVITACION:" + anfitrion + ":" + sala.getId());
                cliente.enviarMensaje("Invitación enviada a " + nombreInvitado);
            } else {
                cliente.enviarMensaje("Error: No estás en una sala.");
            }
        } else {
            cliente.enviarMensaje("Error: El usuario no está conectado.");
        }
    }

    private void handleIniciarPartida() {
        Sala sala = GestorSalas.buscarSalaDeJugador(cliente);
            
        if (sala != null) {
            if (sala.isJuegoIniciado()) {
                cliente.enviarMensaje("Error: El juego ya está activo. No puedes iniciarlo de nuevo.");
                return;
            }

            if (!sala.esAnfitrion(cliente)) { 
                cliente.enviarMensaje("Error: Solo el anfitrión puede iniciar la partida.");
                return;
            }
            
            // Si todo está bien, inicia el juego
            sala.iniciarJuego();
            
        } else {
            cliente.enviarMensaje("Error: No estás en ninguna sala.");
        }
    }

    private void handleSeleccionObjetivo(String[] partes) {
        if (partes.length < 2) return;
        String victima = partes[1];
        
        Sala sala = GestorSalas.buscarSalaDeJugador(cliente);
        if (sala != null) {
            sala.procesarSeleccionObjetivo(cliente, victima);
        }
    }

    private void handleSalirSala() {
        Sala sala = GestorSalas.buscarSalaDeJugador(cliente);
        if (sala != null) {
            sala.removerJugador(cliente);
            cliente.enviarMensaje("SALIDA_EXITOSA: Has salido de la sala.");
        } else {
            cliente.enviarMensaje("Error: No estás en ninguna sala.");
        }
    }
    
    private void handleJugada(String accion) {
        // 1. Buscamos en qué sala está jugando este cliente
        Sala sala = GestorSalas.buscarSalaDeJugador(cliente);
        
        if (sala != null && sala.isJuegoIniciado()) {
            sala.procesarAccionJugador(cliente, accion);
        } else {
            cliente.enviarMensaje("Error: No estás en una partida activa o no es tu turno.");
        }
    }
    
    private void handleVotoFinJuego(String comando) {
        Sala sala = GestorSalas.buscarSalaDeJugador(cliente);

        if (sala != null && !sala.isJuegoIniciado()) {
            // Utilizamos el mismo comando 'REINICIAR' o traducimos 'VOTAR_SALIR' a 'SALIR_SALA'
            String voto = comando.equals("VOTAR_SALIR") ? "SALIR_SALA" : "REINICIAR";
            sala.procesarVoto(cliente, voto);
        } else {
            cliente.enviarMensaje("Error: No puedes votar ahora.");
        }
    }

    private void handleGuardarPartida() {
        Sala sala = GestorSalas.buscarSalaDeJugador(cliente);
        if (sala != null) {
            sala.guardarPartida(db, cliente);
        } else {
            cliente.enviarMensaje("Error: No estás en una sala para guardar.");
        }
    }

    // NUEVO: Lógica para cargar partida
    private void handleCargarPartida(String[] partes) {
        if (partes.length < 2) {
            cliente.enviarMensaje("Error: Debes escribir el ID de la partida.");
            return;
        }

        int idPartida;
        try {
            idPartida = Integer.parseInt(partes[1]);
        } catch (NumberFormatException e) {
            cliente.enviarMensaje("Error: El ID debe ser un número.");
            return;
        }

        // 1. Recuperar datos de la BD
        java.util.Map<String, Integer> datos = db.cargarDatosPartida(idPartida);
        
        if (datos.isEmpty()) {
            cliente.enviarMensaje("Error: No se encontró ninguna partida con ese ID o estaba vacía.");
            return;
        }

        // 2. Verificar si el usuario que intenta cargar estaba en esa partida
        if (!datos.containsKey(cliente.getUsuarioActual())) {
            cliente.enviarMensaje("Advertencia: Tu usuario no figura en esa partida guardada, pero la sala se creará igual.");
        }

        // 3. Crear la sala (Reutilizamos la lógica de crear sala)
        handleCrearSala(); 

        // 4. Buscar la sala recién creada y "pegarle" los datos viejos
        Sala sala = GestorSalas.buscarSalaDeJugador(cliente);
        if (sala != null) {
            sala.establecerDatosCargados(datos);
            cliente.enviarMensaje("--- PARTIDA RECUPERADA EXITOSAMENTE ---");
            cliente.enviarMensaje("Invita a tus amigos. Cuando se unan, recuperarán sus puntos automáticamente.");
        }
    }
}