package com.example.appinventario;

import java.io.Serializable;
import java.util.List;

public class Producto implements Serializable {
    private int codigo;
    private String descripcion;
    private double precio;
    private double precioOriginal;
    private double porcentajeDescuento;
    private boolean oferta;
    private boolean enOferta;
    private String familiaOlfativa;
    private List<String> familiasOlfativas;
    private String publicoObjetivo;
    private String urlFoto;
    private String imagenUrl;
    private String imagenBase64;
    private List<String> fotosEvidencia;
    private String marca;
    private String concentracion;
    private String volumen;
    private int stock = 1;
    private double latitud;
    private double longitud;
    private String direccion;

    public Producto() {}

    public Producto(int codigo, String descripcion, double precio, boolean oferta) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.precio = precio;
        this.precioOriginal = precio;
        this.oferta = oferta;
        this.stock = 1;
        this.marca = "General";
        this.concentracion = "EDP";
        this.volumen = "100 ml";
    }

    public Producto(int codigo, String descripcion, String marca, String concentracion,
                    String volumen, double precio, int stock, boolean oferta,
                    double latitud, double longitud, String direccion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.marca = marca;
        this.concentracion = concentracion;
        this.volumen = volumen;
        this.precio = precio;
        this.precioOriginal = precio;
        this.stock = stock;
        this.oferta = oferta;
        this.latitud = latitud;
        this.longitud = longitud;
        this.direccion = direccion;
    }

    public int getCodigo() { return codigo; }
    public void setCodigo(int codigo) { this.codigo = codigo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public double getPrecioOriginal() { return precioOriginal; }
    public void setPrecioOriginal(double precioOriginal) { this.precioOriginal = precioOriginal; }

    public double getPorcentajeDescuento() { return porcentajeDescuento; }
    public void setPorcentajeDescuento(double porcentajeDescuento) { this.porcentajeDescuento = porcentajeDescuento; }

    public String getFamiliaOlfativa() { return familiaOlfativa; }
    public void setFamiliaOlfativa(String familiaOlfativa) { this.familiaOlfativa = familiaOlfativa; }

    public List<String> getFamiliasOlfativas() { return familiasOlfativas; }
    public void setFamiliasOlfativas(List<String> familiasOlfativas) { this.familiasOlfativas = familiasOlfativas; }

    public String getPublicoObjetivo() { return publicoObjetivo; }
    public void setPublicoObjetivo(String publicoObjetivo) { this.publicoObjetivo = publicoObjetivo; }

    public boolean isOferta() { return oferta || enOferta; }
    public void setOferta(boolean oferta) { this.oferta = oferta; }
    public boolean isEnOferta() { return enOferta; }
    public void setEnOferta(boolean enOferta) { this.enOferta = enOferta; }

    public String getUrlFoto() { return urlFoto; }
    public void setUrlFoto(String urlFoto) { this.urlFoto = urlFoto; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public String getImagenBase64() { return imagenBase64; }
    public void setImagenBase64(String imagenBase64) { this.imagenBase64 = imagenBase64; }

    public List<String> getFotosEvidencia() { return fotosEvidencia; }
    public void setFotosEvidencia(List<String> fotosEvidencia) { this.fotosEvidencia = fotosEvidencia; }

    // Getters y Setters de Perfumería
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getConcentracion() { return concentracion; }
    public void setConcentracion(String concentracion) { this.concentracion = concentracion; }

    public String getVolumen() { return volumen; }
    public void setVolumen(String volumen) { this.volumen = volumen; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    // Getters y Setters de Ubicación
    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }

    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
}