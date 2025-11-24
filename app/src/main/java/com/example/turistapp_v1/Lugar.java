package com.example.turistapp_v1;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

// Esta anotación hace que la app ignore cualquier campo extra en la base de datos que no esté definido aquí.
// Esto evita que la app se cierre si en el futuro se añaden nuevos campos a la base de datos.
@IgnoreExtraProperties
public class Lugar {

    // Se inicializan los campos con valores por defecto. Si Firebase no encuentra un valor para un campo,
    // usará este valor en lugar de "null", lo que previene muchos cierres inesperados (crashes).
    private String id;
    private String nombre = "";
    private String descripcion = "";
    private String urlImagen = "";
    private String region = "";
    private String categoria = "";
    private Double latitud = 0.0;
    private Double longitud = 0.0;
    private String direccion = "";
    private String creadoPorAdminId = "";
    private Long fechaCreacion = 0L;
    private Integer costoAproximado = 0;
    private String horario = "";

    public Lugar() {
    }

    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @PropertyName("nombre")
    public String getNombre() {
        return nombre;
    }

    @PropertyName("nombre")
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    @PropertyName("descripcion")
    public String getDescripcion() {
        return descripcion;
    }

    @PropertyName("descripcion")
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    @PropertyName("urlImagen")
    public String getUrlImagen() {
        return urlImagen;
    }

    @PropertyName("urlImagen")
    public void setUrlImagen(String urlImagen) {
        this.urlImagen = urlImagen;
    }

    @PropertyName("region")
    public String getRegion() {
        return region;
    }

    @PropertyName("region")
    public void setRegion(String region) {
        this.region = region;
    }

    @PropertyName("categoria")
    public String getCategoria() {
        return categoria;
    }

    @PropertyName("categoria")
    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    @PropertyName("latitud")
    public Double getLatitud() {
        return latitud;
    }

    @PropertyName("latitud")
    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    @PropertyName("longitud")
    public Double getLongitud() {
        return longitud;
    }

    @PropertyName("longitud")
    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }

    @PropertyName("direccion")
    public String getDireccion() {
        return direccion;
    }

    @PropertyName("direccion")
    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    @PropertyName("creadoPorAdminId")
    public String getCreadoPorAdminId() {
        return creadoPorAdminId;
    }

    @PropertyName("creadoPorAdminId")
    public void setCreadoPorAdminId(String creadoPorAdminId) {
        this.creadoPorAdminId = creadoPorAdminId;
    }

    @PropertyName("fechaCreacion")
    public Long getFechaCreacion() {
        return fechaCreacion;
    }

    @PropertyName("fechaCreacion")
    public void setFechaCreacion(Long fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    @PropertyName("costoAproximado")
    public Integer getCostoAproximado() {
        return costoAproximado;
    }

    @PropertyName("costoAproximado")
    public void setCostoAproximado(Integer costoAproximado) {
        this.costoAproximado = costoAproximado;
    }

    @PropertyName("horario")
    public String getHorario() {
        return horario;
    }

    @PropertyName("horario")
    public void setHorario(String horario) {
        this.horario = horario;
    }
}
