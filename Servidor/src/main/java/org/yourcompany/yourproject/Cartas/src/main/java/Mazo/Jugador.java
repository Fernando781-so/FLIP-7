package org.yourcompany.yourproject.Cartas.src.main.java.Mazo;

import java.util.ArrayList;
import java.util.List;


public class Jugador {
    private String nombre;        
    private List<Carta> mano;     
    private int puntaje;         
    private boolean enLinea;      
    private boolean jugando;      

    public Jugador(String nombre) {
        this.nombre = nombre;
        this.mano = new ArrayList<>();
        this.puntaje = 0;
        this.enLinea = true;      
        this.jugando = false;
    }

    
    public void recibirCarta(Carta carta) {
        this.mano.add(carta);
       
    }

    public String getNombre() {
        return nombre;
    }

    public List<Carta> getMano() {
        return mano;
    }

    public int getPuntaje() {
        return puntaje;
    }

    public void setPuntaje(int puntaje) {
        this.puntaje = puntaje;
    }
    
    
    public void calcularPuntaje() {
        int suma = 0;
        for (Carta c : this.mano) {
            suma += c.getValor();
        }
        this.puntaje = suma;
    }

}