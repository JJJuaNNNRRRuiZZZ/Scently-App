package com.example.appinventario;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;
import android.os.Looper;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class UbicacionActivity extends AppCompatActivity{

    private TextView tvLatitud, tvLongitud, tvDireccion;
    private Button btnObtenerUbicacion;
    private SwitchMaterial swRastreoTiempoReal;

    // 1. El cliente principal de Google Play Services
    private FusedLocationProviderClient fusedLocationClient;

    // 2. Launcher para permisos múltiples (Preciso + Aproximado)
    private ActivityResultLauncher<String[]> solicitarPermisosUbicacionLauncher;

    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private boolean rastreoActivo = false;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ubicacion);

        tvLatitud = findViewById(R.id.tvLatitud);
        tvLongitud = findViewById(R.id.tvLongitud);
        tvDireccion = findViewById(R.id.tvDireccion);
        btnObtenerUbicacion = findViewById(R.id.btnObtenerUbicacion);
        swRastreoTiempoReal = findViewById(R.id.swRastreoTiempoReal);

        // Inicializar el cliente de ubicación de Google
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // =========================================================================
        // CONFIGURACIÓN DEL LAUNCHER DE PERMISOS
        // =========================================================================
        solicitarPermisosUbicacionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                    if (fineLocationGranted != null && fineLocationGranted) {
                        Toast.makeText(this, "Permiso de GPS Preciso concedido", Toast.LENGTH_SHORT).show();
                        obtenerUltimaUbicacion();
                    } else if (coarseLocationGranted != null && coarseLocationGranted) {
                        Toast.makeText(this, "Permiso de Ubicación Aproximada concedido", Toast.LENGTH_SHORT).show();
                        obtenerUltimaUbicacion();
                    } else {
                        Toast.makeText(this, "Permisos de ubicación denegados", Toast.LENGTH_LONG).show();
                    }
                }
        );

        // Evento del botón para ubicación estática
        btnObtenerUbicacion.setOnClickListener(v -> verificarPermisosYObtenerUbicacion());

        configurarRastreoEnTiempoReal();

        swRastreoTiempoReal.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                iniciarActualizacionesUbicacion();
            } else {
                detenerActualizacionesUbicacion();
            }
        });
    }

    private void verificarPermisosYObtenerUbicacion(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Ya tenemos permisos
            obtenerUltimaUbicacion();
        } else {
            // Pedimos ambos permisos al tiempo
            solicitarPermisosUbicacionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    // =========================================================================
    // MÉTODO A: Obtener la última coordenada en caché (Rápido, 0 consumo de batería)
    // =========================================================================
    private void obtenerUltimaUbicacion(){
        // Verificación formal requerida por el IDE
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                actualizarUI(location);
            } else {
                Toast.makeText(this, "No se encontró ubicación previa en caché. Activa el GPS.", Toast.LENGTH_LONG).show();
            }
        });
    }

    // Método auxiliar para pintar en pantalla y traducir a calle
    private void actualizarUI(Location location){
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        tvLatitud.setText("Latitud: " + lat);
        tvLongitud.setText("Longitud: " + lon);

        // Geocodificación inversa: Convertir coordenadas a nombre de calle
        convertirCoordenadasADireccion(lat, lon);
    }

    private void convertirCoordenadasADireccion(double lat, double lon){
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> direcciones = geocoder.getFromLocation(lat, lon, 1);
            if (direcciones != null && !direcciones.isEmpty()) {
                Address dir = direcciones.get(0);
                // Obtenemos la dirección completa (ej. "Cra. 7 #72-41, Bogotá")
                String direccionCompleta = dir.getAddressLine(0);
                tvDireccion.setText(direccionCompleta);
            } else {
                tvDireccion.setText("Dirección no disponible para estas coordenadas.");
            }
        } catch (IOException e) {
            tvDireccion.setText("Error al consultar el servicio de Geocoder: " + e.getMessage());
        }
    }

    private void configurarRastreoEnTiempoReal() {
        // Pedimos alta precisión (GPS) cada 5 segundos (5000 ms), con un mínimo de 2 segundos (2000 ms)
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        // El Callback escucha las nuevas coordenadas conforme el usuario se desplaza
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                for (Location location : locationResult.getLocations()) {
                    // Se ejecuta cada 5 segundos si el teléfono detecta movimiento
                    actualizarUI(location);
                }
            }
        };
    }

    private void iniciarActualizacionesUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            swRastreoTiempoReal.setChecked(false);
            Toast.makeText(this, "Se requiere permiso preciso para rastreo", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        rastreoActivo = true;
        Toast.makeText(this, "Rastreo GPS en tiempo real activado", Toast.LENGTH_SHORT).show();
    }

    private void detenerActualizacionesUbicacion() {
        if (rastreoActivo) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            rastreoActivo = false;
            Toast.makeText(this, "Rastreo GPS detenido para ahorrar batería", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (rastreoActivo) {
            detenerActualizacionesUbicacion();
            swRastreoTiempoReal.setChecked(false);
        }
    }
}