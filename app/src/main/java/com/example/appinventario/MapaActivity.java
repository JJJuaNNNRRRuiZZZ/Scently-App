package com.example.appinventario;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class MapaActivity extends AppCompatActivity {

    private static final GeoPoint BOUTIQUE_SCENTLY = new GeoPoint(6.2442, -75.5812);
    private static final int REQUEST_LOCATION_PERMISSIONS = 4101;
    private static final String PREF_LOCATION_REQUESTED = "location_permission_requested";

    private MapView mapView;
    private Button btnVolverMapa;
    private Button btnTipoMapa;
    private Button btnCapas;
    private android.widget.TextView tvAtribucionMapa;
    private boolean esSatelite = false;
    private MyLocationNewOverlay myLocationOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(
                this,
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        );
        Configuration.getInstance().setUserAgentValue("ScentlyApp_LiveGPS_v1");
        setContentView(R.layout.activity_mapa);

        mapView = findViewById(R.id.mapView);
        tvAtribucionMapa = findViewById(R.id.tvAtribucionMapa);
        mapView.setTileSource(new org.osmdroid.tileprovider.tilesource.XYTileSource(
                "OSMFrance",
                0,
                19,
                256,
                ".png",
                new String[]{
                        "https://a.tile.openstreetmap.fr/osmfr/",
                        "https://b.tile.openstreetmap.fr/osmfr/",
                        "https://c.tile.openstreetmap.fr/osmfr/"
                }
        ));
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(16.0);
        mapView.getController().setCenter(new GeoPoint(6.2442, -75.5812));

        btnVolverMapa = findViewById(R.id.btnVolverMapa);
        btnTipoMapa = findViewById(R.id.btnTipoMapa);
        btnCapas = findViewById(R.id.btnCapas);
        btnVolverMapa.setOnClickListener(v -> finish());
        btnTipoMapa.setOnClickListener(v -> centrarEnUbicacionActual());
        btnCapas.setOnClickListener(v -> alternarCapaMapa());

        solicitarPermisoUbicacionSiHaceFalta();
        activarSeguimientoGps();
        cargarMarcadoresDeProductos();
    }

    private void alternarCapaMapa() {
        if (!esSatelite) {
            mapView.setTileSource(new org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase(
                    "EsriWorldImagery",
                    0,
                    19,
                    256,
                    "",
                    new String[]{"https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"}
            ) {
                @Override
                public String getTileURLString(long tileIndex) {
                    return getBaseUrl()
                            + org.osmdroid.util.MapTileIndex.getZoom(tileIndex) + "/"
                            + org.osmdroid.util.MapTileIndex.getY(tileIndex) + "/"
                            + org.osmdroid.util.MapTileIndex.getX(tileIndex);
                }
            });
            tvAtribucionMapa.setText(R.string.map_attribution_satellite);
            Toast.makeText(this, "Capa Satelital activada", Toast.LENGTH_SHORT).show();
            esSatelite = true;
        } else {
            mapView.setTileSource(new org.osmdroid.tileprovider.tilesource.XYTileSource(
                    "OsmFr",
                    0,
                    19,
                    256,
                    ".png",
                    new String[]{"https://a.tile.openstreetmap.fr/osmfr/"}
            ));
            tvAtribucionMapa.setText(R.string.map_attribution);
            Toast.makeText(this, "Capa Calles activada", Toast.LENGTH_SHORT).show();
            esSatelite = false;
        }
        mapView.invalidate();
    }

    private void solicitarPermisoUbicacionSiHaceFalta() {
        if (tienePermisoGps()) {
            activarSeguimientoGps();
            return;
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Ubicación en tiempo real")
                    .setMessage("Scently necesita tu ubicación para centrar el mapa y seguir tu recorrido.")
                    .setPositiveButton("Continuar", (dialog, which) -> solicitarPermisosGps())
                    .setNegativeButton("Ahora no", (dialog, which) -> dialog.dismiss())
                    .show();
        } else {
            solicitarPermisosGps();
        }
    }

    private void solicitarPermisosGps() {
        getPreferences(MODE_PRIVATE)
                .edit()
                .putBoolean(PREF_LOCATION_REQUESTED, true)
                .apply();

        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                REQUEST_LOCATION_PERMISSIONS
        );
    }

    private boolean tienePermisoGps() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void activarSeguimientoGps() {
        if (mapView == null || !tienePermisoGps()) {
            return;
        }

        if (myLocationOverlay == null) {
            myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
            myLocationOverlay.setDrawAccuracyEnabled(true);
            Bitmap personIcon = bitmapFromVector(R.drawable.ic_gps_location_dot);
            if (personIcon != null) {
                myLocationOverlay.setPersonIcon(personIcon);
                myLocationOverlay.setPersonAnchor(0.5f, 0.5f);
            }
            Bitmap directionIcon = bitmapFromVector(R.drawable.ic_gps_direction_arrow);
            if (directionIcon != null) {
                myLocationOverlay.setDirectionIcon(directionIcon);
                myLocationOverlay.setDirectionAnchor(0.5f, 0.5f);
            }
            mapView.getOverlays().add(myLocationOverlay);
        }

        try {
            myLocationOverlay.enableMyLocation();
            myLocationOverlay.enableFollowLocation();
            mapView.invalidate();
        } catch (SecurityException error) {
            Toast.makeText(this, "No se pudo acceder a la ubicación GPS", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap bitmapFromVector(int drawableResource) {
        Drawable drawable = ContextCompat.getDrawable(this, drawableResource);
        if (drawable == null) {
            return null;
        }

        int size = (int) (48 * getResources().getDisplayMetrics().density);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, size, size);
        drawable.draw(canvas);
        return bitmap;
    }

    private void centrarEnUbicacionActual() {
        if (!tienePermisoGps()) {
            solicitarPermisoUbicacionSiHaceFalta();
            return;
        }

        if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
            mapView.getController().animateTo(myLocationOverlay.getMyLocation());
            mapView.getController().setZoom(17.0);
            myLocationOverlay.enableFollowLocation();
        } else {
            activarSeguimientoGps();
            Toast.makeText(this, "Buscando señal GPS…", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_LOCATION_PERMISSIONS) {
            return;
        }

        if (tienePermisoGps()) {
            activarSeguimientoGps();
            return;
        }

        boolean yaSeSolicito = getPreferences(MODE_PRIVATE)
                .getBoolean(PREF_LOCATION_REQUESTED, false);
        boolean puedeVolverASolicitar =
                ActivityCompat.shouldShowRequestPermissionRationale(
                        this, Manifest.permission.ACCESS_FINE_LOCATION)
                || ActivityCompat.shouldShowRequestPermissionRationale(
                        this, Manifest.permission.ACCESS_COARSE_LOCATION);

        if (yaSeSolicito && !puedeVolverASolicitar) {
            new AlertDialog.Builder(this)
                    .setTitle("Permiso de ubicación desactivado")
                    .setMessage("Activa la ubicación precisa en Ajustes para usar el seguimiento GPS.")
                    .setPositiveButton("Abrir ajustes", (dialog, which) -> {
                        Intent ajustes = new Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", getPackageName(), null)
                        );
                        startActivity(ajustes);
                    })
                    .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                    .show();
        } else {
            Toast.makeText(this, "Se requiere ubicación precisa para el seguimiento GPS",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void cargarMarcadoresDeProductos() {
        FirebaseFirestore.getInstance()
                .collection("productos")
                .get()
                .addOnSuccessListener(productos -> {
                    boolean hayCoordenadas = false;
                    GeoPoint primeraUbicacion = null;

                    for (QueryDocumentSnapshot documento : productos) {
                        Double latitud = obtenerCoordenada(documento, "latitud", "latitude", "lat");
                        Double longitud = obtenerCoordenada(documento, "longitud", "longitude", "lng");

                        if (latitud == null || longitud == null
                                || latitud == 0d || longitud == 0d
                                || latitud < -90 || latitud > 90
                                || longitud < -180 || longitud > 180) {
                            continue;
                        }

                        GeoPoint punto = new GeoPoint(latitud, longitud);
                        String titulo = documento.getString("descripcion");
                        if (titulo == null || titulo.trim().isEmpty()) {
                            titulo = "Fragancia Scently";
                        }

                        Object precio = documento.get("precio");
                        Marker marcador = new Marker(mapView);
                        marcador.setPosition(punto);
                        marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        marcador.setTitle(titulo);
                        marcador.setSnippet(construirSnippet(precio));
                        mapView.getOverlays().add(marcador);

                        if (primeraUbicacion == null) {
                            primeraUbicacion = punto;
                        }
                        hayCoordenadas = true;
                    }

                    if (hayCoordenadas) {
                        if (!tienePermisoGps()) {
                            mapView.getController().setZoom(16.0);
                            mapView.getController().setCenter(primeraUbicacion);
                        }
                    } else {
                        agregarMarcadorBoutique();
                    }
                    mapView.invalidate();
                })
                .addOnFailureListener(error -> {
                    agregarMarcadorBoutique();
                    mapView.invalidate();
                    Toast.makeText(this, "No se pudieron cargar las ubicaciones guardadas",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private Double obtenerCoordenada(QueryDocumentSnapshot documento, String... campos) {
        for (String campo : campos) {
            Object valor = documento.get(campo);
            if (valor instanceof Number) {
                double coordenada = ((Number) valor).doubleValue();
                if (!Double.isNaN(coordenada) && !Double.isInfinite(coordenada)) {
                    return coordenada;
                }
            }
        }
        return null;
    }

    private String construirSnippet(Object precio) {
        return precio == null ? "Precio no disponible" : "Precio: $ " + precio;
    }

    private void agregarMarcadorBoutique() {
        if (mapView == null) {
            return;
        }

        Marker boutique = new Marker(mapView);
        boutique.setPosition(BOUTIQUE_SCENTLY);
        boutique.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        boutique.setTitle("Boutique Principal Scently");
        boutique.setSnippet("Punto de referencia Scently");
        mapView.getOverlays().add(boutique);
        if (!tienePermisoGps()) {
            mapView.getController().setZoom(16.0);
            mapView.getController().setCenter(BOUTIQUE_SCENTLY);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
            if (tienePermisoGps()) {
                activarSeguimientoGps();
            }
        }
    }

    @Override
    protected void onPause() {
        if (myLocationOverlay != null) {
            myLocationOverlay.disableFollowLocation();
            myLocationOverlay.disableMyLocation();
        }
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }
}
