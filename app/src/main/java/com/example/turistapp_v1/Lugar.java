package com.example.turistapp_v1;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import com.google.firebase.Timestamp;

import java.util.Date;

public class Lugar {
    private String id;
    private String nombre;
    private String descripcion;
    private String urlImagen;
    private String region;
    private String categoria;
    private double latitud;
    private double longitud;
    private String direccion;
    private String creadorId;

    @ServerTimestamp
    private Timestamp fechaCreacionTimestamp; // Para Firestore - este es el campo que Firestore usa

    // --- NUEVOS CAMPOS PARA EL RATING ---
    private double ratingPromedio;
    private int cantidadRatings;
    // ------------------------------------

    public Lugar() {
        // Constructor vacío requerido por Firestore
    }

    public Lugar(String nombre, String descripcion, String urlImagen, String region, String categoria, double latitud, double longitud, String direccion, String creadorId) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.urlImagen = urlImagen;
        this.region = region;
        this.categoria = categoria;
        this.latitud = latitud;
        this.longitud = longitud;
        this.direccion = direccion;
        this.creadorId = creadorId;
        this.ratingPromedio = 0.0;
        this.cantidadRatings = 0;
    }

    @Exclude
    public long getFechaCreacion() {
        if (fechaCreacionTimestamp != null) {
            return fechaCreacionTimestamp.getSeconds() * 1000 + fechaCreacionTimestamp.getNanoseconds() / 1000000;
        }
        return 0;
    }

    @Exclude
    public void setFechaCreacion(long fechaCreacion) {
        // Convertir long a Timestamp si es necesario
        if (fechaCreacion > 0) {
            this.fechaCreacionTimestamp = new Timestamp(new Date(fechaCreacion));
        }
    }

    public Timestamp getFechaCreacionTimestamp() {
        return fechaCreacionTimestamp;
    }

    public void setFechaCreacionTimestamp(Timestamp fechaCreacionTimestamp) {
        this.fechaCreacionTimestamp = fechaCreacionTimestamp;
    }

    // ... (resto de getters y setters existentes)

    // --- NUEVOS GETTERS Y SETTERS PARA EL RATING ---
    public double getRatingPromedio() {
        return ratingPromedio;
    }

    public void setRatingPromedio(double ratingPromedio) {
        this.ratingPromedio = ratingPromedio;
    }

    public int getCantidadRatings() {
        return cantidadRatings;
    }

    public void setCantidadRatings(int cantidadRatings) {
        this.cantidadRatings = cantidadRatings;
    }
    // ------------------------------------------------

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getUrlImagen() {
        return urlImagen;
    }

    public void setUrlImagen(String urlImagen) {
        this.urlImagen = urlImagen;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public double getLatitud() {
        return latitud;
    }

    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getCreadorId() {
        return creadorId;
    }

    public void setCreadorId(String creadorId) {
        this.creadorId = creadorId;
    }
}