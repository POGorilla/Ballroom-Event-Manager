package model;

import java.time.LocalDate;

public class EvenimentTabelModel {

    private int id;
    private LocalDate data;
    private String denumire;
    private String client;
    private String sala;
    private String status;
    private double pretTotal;

    public EvenimentTabelModel(int id, LocalDate data, String denumire, String client, String sala, String status, double pretTotal) {
        this.id = id;
        this.data = data;
        this.denumire = denumire;
        this.client = client;
        this.sala = sala;
        this.status = status;
        this.pretTotal = pretTotal;
    }

    public int getId() { return id; }
    public LocalDate getData() { return data; }
    public String getDenumire() { return denumire; }
    public String getClient() { return client; }
    public String getSala() { return sala; }
    public String getStatus() { return status; }
    public double getPretTotal() { return pretTotal; }
}
