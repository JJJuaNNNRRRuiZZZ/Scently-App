package com.example.appinventario;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
public interface ApiService {

    @GET("products/{id}")
    Call<ProductoAPI> obtenerProducto(@Path("id") int productoId);
}
