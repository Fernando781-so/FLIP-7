package org.yourcompany.yourproject.Cartas.src.main.java.Mazo;

import java.util.ArrayList;
import java.util.List;

public class Jugador {
    private String nombre;        
    private List<Carta> mano;     
    private int puntajeRonda; 
    private int puntajeTotal; 
    private boolean plantado; 
    private boolean eliminadoRonda; 

    public Jugador(String nombre) {
        this.nombre = nombre;
        this.mano = new ArrayList<>();
        this.puntajeRonda = 0;
        this.puntajeTotal = 0;
        this.plantado = false;
        this.eliminadoRonda = false;
    }

    public void recibirCarta(Carta carta) {
        this.mano.add(carta);
    }

    public String getNombre() { return nombre; }
    public List<Carta> getMano() { return mano; }
    
    // Gestión de Puntos
    public int getPuntajeRonda() { return puntajeRonda; }
    public void setPuntajeRonda(int p) { this.puntajeRonda = p; }
    
    public int getPuntajeTotal() { return puntajeTotal; }
    public void agregarPuntajeTotal(int p) { this.puntajeTotal += p; }

    // Gestión de Estado
    public boolean isPlantado() { return plantado; }
    public void setPlantado(boolean plantado) { this.plantado = plantado; }

    public boolean isEliminadoRonda() { return eliminadoRonda; }
    public void setEliminadoRonda(boolean eliminado) { this.eliminadoRonda = eliminado; }

    public void calcularPuntajeRonda() {
        int sumaNumeros = 0;
        int sumaBonos = 0;
        int multiplicador = 1;

        for (Carta c : this.mano) {
            if (c.getTipo() == TipoAccion.NUMERO) {
                sumaNumeros += c.getValor();
            } 
            else if (c.getTipo() == TipoAccion.PUNTOS) {
                sumaBonos += c.getValor(); 
            }
            else if (c.getTipo() == TipoAccion.MULTIPLICADOR) {
                multiplicador *= c.getValor(); 
            }
        }
        this.puntajeRonda = (sumaNumeros * multiplicador) + sumaBonos;
    }

    public int getCantidadNumericas() {
        int conteo = 0;
        for(Carta c : mano) {
            if(c.getTipo() == TipoAccion.NUMERO) conteo++;
        }
        return conteo;
    }


    public void reiniciarRonda() {
        this.mano.clear();
        this.puntajeRonda = 0;
        this.plantado = false;
        this.eliminadoRonda = false;
    }
}
