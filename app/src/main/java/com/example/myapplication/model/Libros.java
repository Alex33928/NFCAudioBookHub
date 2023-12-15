package com.example.myapplication.model;

public class Libros {
    private String uid;
    private String titulos;
    private String autores;
    private String impresos;
    private String brailles;
    private String multimedias;

    public String getTitulos() {
        return titulos;
    }

    public void setTitulos(String titulos) {
        this.titulos = titulos;
    }

    public String getAutores() {
        return autores;
    }

    public void setAutores(String autores) {
        this.autores = autores;
    }

    public String getImpresos() {
        return impresos;
    }

    public void setImpresos(String impresos) {
        this.impresos = impresos;
    }

    public String getBrailles() {
        return brailles;
    }

    public void setBrailles(String brailles) {
        this.brailles = brailles;
    }

    public String getMultimedias() {
        return multimedias;
    }

    public void setMultimedias(String multimedias) {
        this.multimedias = multimedias;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    @Override
    public String toString(){
        return "                           Titulo:"+titulos+
                ""+
               "                           Autor:"+ autores+
               "      Disponible Impreso:"+ impresos+
                "                          Disponible Braille:"+ brailles+
               "                           Disponible Multimedia:"+ multimedias;
    }
}
