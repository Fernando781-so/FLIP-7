package Mazo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Mazo {
    private final List<Carta> cartas;
    public Mazo() {
        this.cartas = new ArrayList<>();
        
        this.cartas.add(new Carta(0)); 

        for (int valor = 1; valor <= 12; valor++) {
            for (int i = 0; i < valor; i++) {
                this.cartas.add(new Carta(valor));
            }
        }
    }
    public void barajar() {
        Collections.shuffle(this.cartas);
    }
    public int getTamano() {
        return this.cartas.size();
    }
}
