package com.example.appinventario;

public class Usuario {
    private String idDocumento;
    private String nombre;
    private String correo;
    private String rol;

    private Usuario() {}

    //Getters
    public String getIdDocumento() { return idDocumento; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getRol() { return rol; }

    //Setters
    public void setIdDocumento(String idDocumento) { this.idDocumento = idDocumento; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setCorreo(String correo) { this.correo = correo; }
    public void setRol(String rol) { this.rol = rol; }
}
