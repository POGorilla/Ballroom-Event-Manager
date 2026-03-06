package model;

public class SalaModel {
    private int id;
    private String denumire;
    private double pretOra;

    public SalaModel(int id, String denumire, double pretOra) {
        this.id = id;
        this.denumire = denumire;
        this.pretOra = pretOra;
    }

    public int getId() { return id; }

    public double getPretOra() { return pretOra; }

    @Override
    public String toString() {
        return denumire + " (" + pretOra + " RON/oră)";
    }
}