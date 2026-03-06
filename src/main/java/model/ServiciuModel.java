package model;

public class ServiciuModel {
    private int id;
    private String denumire;
    private double pret;

    public ServiciuModel(int id, String denumire, double pret) {
        this.id = id;
        this.denumire = denumire;
        this.pret = pret;
    }

    public int getId() { return id; }
    public double getPret() { return pret; }
    public String getDenumire() { return denumire; }

    @Override
    public String toString() {
        return denumire + " (" + pret + " RON)";
    }
}