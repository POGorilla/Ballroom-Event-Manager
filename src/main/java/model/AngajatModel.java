package model;

public class AngajatModel {
    private int id;
    private String numeComplet;
    private String functie;

    public AngajatModel(int id, String nume, String prenume, String functie) {
        this.id = id;
        this.numeComplet = nume + " " + prenume;
        this.functie = functie;
    }

    public int getId() { return id; }

    public String getFunctie() {
        return functie;
    }

    @Override
    public String toString() {
        return numeComplet + " (" + functie + ")";
    }
}