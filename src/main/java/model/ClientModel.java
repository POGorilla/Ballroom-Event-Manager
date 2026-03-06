package model;

public class ClientModel {
    private int id;
    private String numeComplet;

    public ClientModel(int id, String numeComplet) {
        this.id = id;
        this.numeComplet = numeComplet;
    }

    public int getId() { return id; }

    @Override
    public String toString() {
        return numeComplet;
    }
}