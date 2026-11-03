package com.example.appinventario;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.Toast;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.WindowManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;
import android.app.PendingIntent;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.core.content.ContextCompat;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.messaging.FirebaseMessaging;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    public static final String CANAL_ID = "canal_alertas_inventario";
    private static final String[] FAMILIAS_OLFATIVAS = {
            "Floral", "Amaderada", "Cítrica", "Oriental / Ámbar",
            "Aromática", "Fresca / Acuática", "Gourmand", "Chipre", "Frutal"
    };
    private static final String[] PUBLICOS_OBJETIVO = {
            "Sin clasificar", "Hombre", "Mujer", "Unisex", "Niño", "Niña", "Infantil"
    };
    private ActivityResultLauncher<String> permisoCamaraLauncher;
    private ActivityResultLauncher<String> permisoUbicacionLauncher;
    private CoordenadasProductoCallback coordenadasPendientes;
    private ActivityResultLauncher<Intent> selectorImagenLauncher;
    private String imagenBase64 = "";
    private ImageView imgProductoPreview;
    private com.google.android.material.button.MaterialButton btnCrearArticulo, btnBuscar, btnEditar, btnBorrar, btnBuscarTodos, btnFiltrar, btnCerrarSesion, btnAutocompletarAPI, btnAdminUsuarios, btnAdjuntarImagen, btnAbrirMapa;
    private EditText etCodigo, etDescripcion, etPrecio, etNombreTienda, etPorcentajeDescuento;
    private EditText etFamiliasOlfativas, etBuscarCatalogo;
    private String busquedaCatalogo = "";
    private Spinner spPublicoObjetivo;
    private com.google.android.material.textfield.TextInputLayout tilPorcentajeDescuento;
    private TextView tvPrecioCalculado;
    private final java.util.LinkedHashSet<String> familiasSeleccionadas = new java.util.LinkedHashSet<>();
    private final java.util.LinkedHashSet<String> familiasFiltro = new java.util.LinkedHashSet<>();
    private String publicoFiltro = "Todos";
    private Double precioMinimoFiltro, precioMaximoFiltro;
    private Button btnGuardarTienda;
    private com.google.android.material.textfield.TextInputLayout tilCodigo, tilDescripcion, tilPrecio;
    private RecyclerView rvProductos;
    private View llEmptyState;
    private AdaptadorProducto adaptador;
    private List<Producto> listaProductos;
    private final List<Producto> catalogoCompleto = new ArrayList<>();
    private boolean filtroOfertasActivo;
    private FirebaseFirestore db;
    private com.google.firebase.firestore.ListenerRegistration listenerFirestore;
    private com.google.android.material.switchmaterial.SwitchMaterial swOferta;
    private android.widget.ProgressBar pbCarga, pbCargaApi;
    private FirebaseAuth mAuth;

    private String nombreCached, rolCached;
    private MaterialToolbar toolbar;
    private View cardFormularioProducto;

    private interface FamiliasSeleccionCallback {
        void onSeleccion(java.util.Set<String> familias);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        crearCanalDeNotificaciones();

        toolbar = findViewById(R.id.toolbar);
        cardFormularioProducto = findViewById(R.id.cardFormularioProducto);
        setSupportActionBar(toolbar);

        etCodigo = findViewById(R.id.etCodigo);
        etBuscarCatalogo = findViewById(R.id.etBuscarCatalogo);
        etDescripcion = findViewById(R.id.etDescripcion);
        etPrecio = findViewById(R.id.etPrecio);
        etNombreTienda = findViewById(R.id.etNombreTienda);

        btnCrearArticulo = findViewById(R.id.btnCrearArticulo);
        btnBuscar = findViewById(R.id.btnBuscar);
        btnEditar = findViewById(R.id.btnEditar);
        btnBorrar = findViewById(R.id.btnBorrar);
        //btnBuscarTodos = findViewById(R.id.btnBuscarTodos);
        btnFiltrar = findViewById(R.id.btnFiltrar);
        rvProductos = findViewById(R.id.rvProductos);
        llEmptyState = findViewById(R.id.llEmptyState);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        //btnGuardarTienda = findViewById(R.id.btnGuardarTienda);
        btnAutocompletarAPI = findViewById(R.id.btnAutocompletarAPI);
        btnAdminUsuarios = findViewById(R.id.btnAdminUsuarios);
        btnAdjuntarImagen = findViewById(R.id.btnAdjuntarImagen);
        imgProductoPreview = findViewById(R.id.imgProductoPreview);
        btnAbrirMapa = findViewById(R.id.btnAbrirMapa);

        tilCodigo = findViewById(R.id.tilCodigo);
        tilDescripcion = findViewById(R.id.tilDescripcion);
        tilPrecio = findViewById(R.id.tilPrecio);

        swOferta = findViewById(R.id.swOferta);
        etFamiliasOlfativas = findViewById(R.id.etFamiliasOlfativas);
        spPublicoObjetivo = findViewById(R.id.spPublicoObjetivo);
        tilPorcentajeDescuento = findViewById(R.id.tilPorcentajeDescuento);
        etPorcentajeDescuento = findViewById(R.id.etPorcentajeDescuento);
        tvPrecioCalculado = findViewById(R.id.tvPrecioCalculado);
        configurarSpinnersClasificacion();
        tilPorcentajeDescuento.setVisibility(View.GONE);
        tvPrecioCalculado.setVisibility(View.GONE);
        pbCarga = findViewById(R.id.pbCarga);
        pbCargaApi = findViewById(R.id.pbCargaApi);

        db = FirebaseFirestore.getInstance();

        mAuth = FirebaseAuth.getInstance();

        int orientacionActual = getResources().getConfiguration().orientation;

        if (orientacionActual == Configuration.ORIENTATION_LANDSCAPE){
            rvProductos.setLayoutManager(new GridLayoutManager(this, 2));
        } else {
            rvProductos.setLayoutManager(new LinearLayoutManager(this));
        }

        /*btnBuscarTodos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cargarListaProductos();
            }
        });*/

        selectorImagenLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                resultado -> {
                    if (resultado.getResultCode() != RESULT_OK || resultado.getData() == null) {
                        return;
                    }
                    Intent datos = resultado.getData();
                    Bitmap bitmap;
                    Uri uri = datos.getData();
                    if (uri != null) {
                        cargarYProcesarImagen(uri);
                        return;
                    }
                    bitmap = obtenerMiniatura(datos);
                    if (bitmap != null) {
                        iniciarProcesamientoImagen(bitmap);
                    } else {
                        Toast.makeText(this, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        permisoCamaraLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                concedido -> {
                    if (concedido) {
                        abrirCamaraProducto();
                    } else {
                        Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        permisoUbicacionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                concedido -> {
                    CoordenadasProductoCallback callback = coordenadasPendientes;
                    coordenadasPendientes = null;
                    if (callback == null) return;
                    if (concedido) {
                        obtenerCoordenadasProducto(callback);
                    } else {
                        callback.onCoordenadas(6.2442, -75.5812);
                    }
                }
        );

        verificarPermisoNotificacion();

        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()){
                    Log.w("FCM_DEBUG", "No se pudo obtener el token de FCM", task.getException());
                    return;
                }
                String tokenDispositivo = task.getResult();
                Log.d("FCM_TOKEN_ACTUAL", "Token para pruebas: " + tokenDispositivo);
            });

        etPrecio.addTextChangedListener(new android.text.TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence sequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence sequence, int start, int before, int count){
                String precioTexto = sequence.toString().trim();
                if (precioTexto.isEmpty()){
                    tilPrecio.setError("El precio no puede estar vacio");
                    return;
                }
                try {
                    if (parsePrecioIngresado(precioTexto) <= 0) {
                        tilPrecio.setError("El precio debe ser mayor a cero");
                    } else {
                        tilPrecio.setErrorEnabled(false);
                    }
                } catch (NumberFormatException error) {
                    tilPrecio.setError("Ingresa un precio numérico válido");
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable sequence) {
                actualizarVistaPreviaDescuento();
            }
        });

        etPorcentajeDescuento.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence sequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence sequence, int start, int before, int count) {
                actualizarVistaPreviaDescuento();
            }

            @Override
            public void afterTextChanged(android.text.Editable sequence) {}
        });

        swOferta.setOnCheckedChangeListener((buttonView, enOferta) -> {
            tilPorcentajeDescuento.setVisibility(enOferta ? View.VISIBLE : View.GONE);
            tvPrecioCalculado.setVisibility(enOferta ? View.VISIBLE : View.GONE);
            actualizarVistaPreviaDescuento();
        });

        etPrecio.setOnFocusChangeListener((view, tieneFoco) -> {
            if (!tieneFoco) {
                formatearPrecioEnCampo();
            }
        });

        btnAbrirMapa.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MapaActivity.class);
            startActivity(intent);
        });

        btnAdjuntarImagen.setOnClickListener(v -> mostrarSelectorImagen());

        btnCrearArticulo.setOnClickListener(v -> {
            if (esFormularioValido()) {
                registrarProductoFirebase();
            }
        });

        btnBuscar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buscarProductoFirebase();
            }
        });

        btnEditar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { actualizarProductoFirebase(); }
        });

        btnBorrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { eliminarProductoFirebase(); }
        });

        etBuscarCatalogo.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {
                buscarProductoFirebase();
                return true;
            }
            return false;
        });

        btnFiltrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { filtrarSoloOfertas(); }
        });

        btnCerrarSesion.setOnClickListener(v -> {

            SharedPreferences prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();

            mAuth.signOut();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        /*btnGuardarTienda.setOnClickListener(v-> {
            String nombreTienda = etNombreTienda.getText().toString().trim();

            if (!nombreTienda.isEmpty()){
                SharedPreferences preferencias = getSharedPreferences("ConfiguracionApp", MODE_PRIVATE);
                SharedPreferences.Editor editor = preferencias.edit();

                editor.putString("nombre_tienda", nombreTienda);
                editor.apply();

                Toast.makeText(MainActivity.this, "Nombre guardado en memoria", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Ingrese el nombre de la tienda", Toast.LENGTH_SHORT).show();
            }
        });*/

        btnAutocompletarAPI.setOnClickListener(v -> obtenerProductosAPI());

        btnAdminUsuarios.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, GestionUsuariosActivity.class));
        });

        SharedPreferences prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE);
        nombreCached = prefs.getString("nombre", "Usuario");
        rolCached = prefs.getString("rol", "Vendedor");

        SharedPreferences preferencias = getSharedPreferences("ConfiguracionApp", MODE_PRIVATE);
        String tiendaGuardada = preferencias.getString("nombre_tienda", "Mi Inventario");

        //etNombreTienda.setText(tiendaGuardada);

        aplicarPermisosDeRol(rolCached);
        configurarSeleccionDeProductos();
        cargarProductosFirebaseTiempoReal();
    }

    private void configurarSpinnersClasificacion() {
        android.widget.ArrayAdapter<String> adaptadorPublicos = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, PUBLICOS_OBJETIVO);
        adaptadorPublicos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPublicoObjetivo.setAdapter(adaptadorPublicos);
        etFamiliasOlfativas.setKeyListener(null);
        etFamiliasOlfativas.setOnClickListener(v -> mostrarSelectorFamilias(
                familiasSeleccionadas, seleccion -> {
                    familiasSeleccionadas.clear();
                    familiasSeleccionadas.addAll(seleccion);
                    actualizarTextoFamiliasFormulario();
                }));
        etFamiliasOlfativas.setOnFocusChangeListener((v, tieneFoco) -> {
            if (tieneFoco) etFamiliasOlfativas.post(() -> etFamiliasOlfativas.clearFocus());
        });
    }

    private void mostrarSelectorFamilias(
            java.util.Set<String> seleccionActual,
            FamiliasSeleccionCallback alConfirmar) {
        boolean[] marcadas = new boolean[FAMILIAS_OLFATIVAS.length];
        for (int i = 0; i < FAMILIAS_OLFATIVAS.length; i++) {
            marcadas[i] = seleccionActual.contains(FAMILIAS_OLFATIVAS[i]);
        }
        java.util.LinkedHashSet<String> seleccionTemporal = new java.util.LinkedHashSet<>(seleccionActual);
        new AlertDialog.Builder(this)
                .setTitle("Selecciona las familias olfativas")
                .setMultiChoiceItems(FAMILIAS_OLFATIVAS, marcadas, (dialog, which, checked) -> {
                    String familia = FAMILIAS_OLFATIVAS[which];
                    if (checked) seleccionTemporal.add(familia);
                    else seleccionTemporal.remove(familia);
                })
                .setPositiveButton("ACEPTAR", (dialog, which) ->
                        alConfirmar.onSeleccion(new java.util.LinkedHashSet<>(seleccionTemporal)))
                .setNeutralButton("LIMPIAR", (dialog, which) -> alConfirmar.onSeleccion(new java.util.LinkedHashSet<>()))
                .setNegativeButton("CANCELAR", null)
                .show();
    }

    private void actualizarTextoFamiliasFormulario() {
        etFamiliasOlfativas.setText(familiasSeleccionadas.isEmpty()
                ? "Seleccionar una o varias familias"
                : android.text.TextUtils.join(", ", familiasSeleccionadas));
    }

    private java.util.List<String> obtenerFamiliasProducto(Producto producto) {
        java.util.List<String> familias = producto.getFamiliasOlfativas();
        if (familias != null && !familias.isEmpty()) return familias;
        String familiaAnterior = producto.getFamiliaOlfativa();
        if (familiaAnterior == null || familiaAnterior.trim().isEmpty()
                || "Sin clasificar".equalsIgnoreCase(familiaAnterior)) return new java.util.ArrayList<>();
        return java.util.Collections.singletonList(familiaAnterior);
    }

    private java.util.List<String> obtenerFamiliasDocumento(com.google.firebase.firestore.DocumentSnapshot documento) {
        Object valor = documento.get("familiasOlfativas");
        java.util.List<String> resultado = new java.util.ArrayList<>();
        if (valor instanceof java.util.List<?>) {
            for (Object familia : (java.util.List<?>) valor) {
                if (familia instanceof String && !((String) familia).trim().isEmpty()) {
                    resultado.add((String) familia);
                }
            }
        }
        if (resultado.isEmpty()) {
            String familiaAnterior = documento.getString("familiaOlfativa");
            if (familiaAnterior != null && !familiaAnterior.trim().isEmpty()
                    && !"Sin clasificar".equalsIgnoreCase(familiaAnterior)) {
                resultado.add(familiaAnterior);
            }
        }
        return resultado;
    }

    private String obtenerSeleccion(Spinner spinner) {
        Object seleccionado = spinner.getSelectedItem();
        return seleccionado == null ? "Sin clasificar" : seleccionado.toString();
    }

    private void seleccionarSpinner(Spinner spinner, String valor) {
        if (valor == null) return;
        android.widget.SpinnerAdapter adaptadorSpinner = spinner.getAdapter();
        for (int indice = 0; indice < adaptadorSpinner.getCount(); indice++) {
            if (valor.equalsIgnoreCase(String.valueOf(adaptadorSpinner.getItem(indice)))) {
                spinner.setSelection(indice);
                return;
            }
        }
        spinner.setSelection(0);
    }

    private double calcularPrecioFinal(double precioOriginal, double porcentaje) {
        if (porcentaje <= 0) return precioOriginal;
        return Math.round(precioOriginal * (100d - porcentaje) / 100d);
    }

    private String formatearMoneda(double precio) {
        java.text.DecimalFormatSymbols simbolos = java.text.DecimalFormatSymbols
                .getInstance(java.util.Locale.forLanguageTag("es-CO"));
        return new java.text.DecimalFormat("$ #,##0", simbolos).format(precio);
    }

    private void actualizarVistaPreviaDescuento() {
        if (tvPrecioCalculado == null || !swOferta.isChecked()) return;
        String precioTexto = etPrecio.getText().toString().trim();
        String descuentoTexto = etPorcentajeDescuento.getText().toString().trim();
        if (precioTexto.isEmpty() || descuentoTexto.isEmpty()) {
            tvPrecioCalculado.setText("Ingresa el precio y el porcentaje de descuento");
            return;
        }
        try {
            double precioOriginal = parsePrecioIngresado(precioTexto);
            double porcentaje = parsePrecioIngresado(descuentoTexto);
            if (porcentaje <= 0 || porcentaje > 100) {
                tvPrecioCalculado.setText("El descuento debe estar entre 1% y 100%");
                return;
            }
            double precioFinal = calcularPrecioFinal(precioOriginal, porcentaje);
            tvPrecioCalculado.setText("Precio con descuento: " + formatearMoneda(precioFinal));
        } catch (NumberFormatException error) {
            tvPrecioCalculado.setText("Revisa el precio y el porcentaje ingresados");
        }
    }

    private double obtenerPorcentajeDescuento() {
        if (!swOferta.isChecked()) return 0d;
        String porcentajeTexto = etPorcentajeDescuento.getText().toString().trim();
        if (porcentajeTexto.isEmpty()) {
            throw new NumberFormatException("Falta el porcentaje de descuento");
        }
        double porcentaje = parsePrecioIngresado(porcentajeTexto);
        if (porcentaje <= 0 || porcentaje > 100) {
            throw new NumberFormatException("Descuento fuera del rango permitido");
        }
        return porcentaje;
    }

    private double parsePrecioIngresado(String precioTexto) {
        String limpio = precioTexto.replace("$", "").replace(" ", "").trim();
        int ultimoPunto = limpio.lastIndexOf('.');
        int ultimaComa = limpio.lastIndexOf(',');

        if (ultimaComa >= 0 && (ultimoPunto < 0 || ultimaComa > ultimoPunto)) {
            limpio = limpio.replace(".", "").replace(',', '.');
        } else if (ultimaComa >= 0) {
            limpio = limpio.replace(",", "");
        } else if (ultimoPunto >= 0
                && limpio.substring(ultimoPunto + 1).length() == 3) {
            limpio = limpio.replace(".", "");
        }
        return Double.parseDouble(limpio);
    }

    private void formatearPrecioEnCampo() {
        String texto = etPrecio.getText().toString().trim();
        if (texto.isEmpty()) return;
        try {
            double precio = parsePrecioIngresado(texto);
            java.text.DecimalFormatSymbols simbolos = java.text.DecimalFormatSymbols
                    .getInstance(java.util.Locale.forLanguageTag("es-CO"));
            java.text.DecimalFormat formato = new java.text.DecimalFormat("#,##0.##", simbolos);
            etPrecio.setText(formato.format(precio));
            etPrecio.setSelection(etPrecio.getText().length());
        } catch (NumberFormatException ignored) {
            // Se conserva el texto para que la validación del formulario muestre el error.
        }
    }

    private boolean esFormularioValido(){
        boolean esValido = true;

        String codigo = etCodigo.getText().toString();
        String desc = etDescripcion.getText().toString();
        String precio = etPrecio.getText().toString();

        if (codigo.isEmpty() || codigo.length() < 3) {
            tilCodigo.setError("El codigo debe tener al menos 3 digitos");
            esValido = false;
        } else {
            tilCodigo.setErrorEnabled(false);
        }

        if (desc.isEmpty() || desc.length() < 10) {
            tilDescripcion.setError("Sea mas descriptivo con el producto (min. 10 caracteres)");
            esValido = false;
        } else {
            tilDescripcion.setErrorEnabled(false);
        }

        try {
            if (precio.trim().isEmpty() || parsePrecioIngresado(precio) <= 0) {
                tilPrecio.setError("El precio debe ser mayor a cero");
                esValido = false;
            } else {
                tilPrecio.setErrorEnabled(false);
            }
        } catch (NumberFormatException error) {
            tilPrecio.setError("Ingresa un precio válido");
            esValido = false;
        }
        if (swOferta.isChecked()) {
            try {
                obtenerPorcentajeDescuento();
                tilPorcentajeDescuento.setErrorEnabled(false);
            } catch (NumberFormatException error) {
                tilPorcentajeDescuento.setError("Ingresa un descuento entre 1% y 100%");
                esValido = false;
            }
        }

        return esValido;
    }
    private void registrarProducto(){
        String codigo = etCodigo.getText().toString();
        String descripcion = etDescripcion.getText().toString();
        String precio = etPrecio.getText().toString();

        if (!codigo.isEmpty() && !descripcion.isEmpty() && !precio.isEmpty()){

            AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this, "administracion.db", null, 1);
            SQLiteDatabase db = admin.getWritableDatabase(); //.getReadableDatabase() Abre la base de datos en solo lectura, getWritableDatabase() abre la base de datos en lectura y escritura.

            ContentValues registro = new ContentValues();
            registro.put("codigo", codigo);
            registro.put("descripcion", descripcion);
            registro.put("precio", precio);

            db.insert("articulos", null, registro);

            //Cerrar conexion a la base de datos por seguridad y por consumo de recursos.
            db.close();

            cargarListaProductos();

            if (adaptador != null){
                adaptador.notifyDataSetChanged();
            }

            etCodigo.setText("");
            etDescripcion.setText("");
            etPrecio.setText("");

            Toast.makeText(MainActivity.this, "Articulo registrado correctamente en base de datos", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "Todos los campos deben estar diligenciados", Toast.LENGTH_SHORT).show();
        }
    }

    private void buscarProducto(){
        String codigo = etCodigo.getText().toString();

        if (!codigo.isEmpty()){
            AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this, "administracion.db", null, 1);
            SQLiteDatabase db = admin.getReadableDatabase();

            android.database.Cursor fila = db.rawQuery("SELECT descripcion, precio FROM articulos WHERE codigo = " + codigo, null);

            if (fila.moveToFirst()){
                etDescripcion.setText(fila.getString(0));
                etPrecio.setText(fila.getString(1));
                Toast.makeText(this, "Producto encontrado", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "El producto no existe", Toast.LENGTH_SHORT).show();
                etCodigo.setText("");
                etDescripcion.setText("");
                etPrecio.setText("");
            }

            db.close();
            fila.close();

        } else {
            Toast.makeText(this, "Ingrese el codigo del producto a buscar", Toast.LENGTH_SHORT).show();
        }
    }

    private void editarProducto(){
        String codigo = etCodigo.getText().toString();
        String descripcion = etDescripcion.getText().toString();
        String precio = etPrecio.getText().toString();

        if (!codigo.isEmpty() && !descripcion.isEmpty() && !precio.isEmpty()){
            AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this, "administracion.db", null, 1);
            SQLiteDatabase db = admin.getWritableDatabase();

            ContentValues registroNuevo = new ContentValues();
            registroNuevo.put("codigo", codigo);
            registroNuevo.put("descripcion", descripcion);
            registroNuevo.put("precio", precio);

            int cantidadActualizadas = db.update("articulos", registroNuevo, "codigo="+codigo, null);

            db.close();

            if (cantidadActualizadas == 1){
                Toast.makeText(MainActivity.this, "Producto actualizado correctamente", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "No se encontro el producto", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, "Debes llenar todos los campos", Toast.LENGTH_SHORT).show();
        }
    }

    private void borrarProducto(){
        String codigo = etCodigo.getText().toString();

        if (!codigo.isEmpty()){
            AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this, "administracion.db", null, 1);
            SQLiteDatabase db = admin.getWritableDatabase();

            int cantidadBorrados = db.delete("articulos", "codigo="+codigo,null);

            db.close();

            etCodigo.setText("");
            etDescripcion.setText("");
            etPrecio.setText("");

            if (cantidadBorrados == 1){
                Toast.makeText(MainActivity.this, "Producto eliminado correctamente", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Producto no encontrado", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, "Ingrese el codigo del producto a eliminar", Toast.LENGTH_SHORT).show();
        }
    }

    private void cargarListaProductos(){
        /*listaProductos = new ArrayList<>();

        AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this, "administracion.db", null, 1);
        SQLiteDatabase db = admin.getReadableDatabase();

        android.database.Cursor fila = db.rawQuery("SELECT codigo, descripcion, precio FROM articulos", null);

        while (fila.moveToNext()){
            int codigo = fila.getInt(0);
            String descripcion = fila.getString(1);
            double precio = fila.getDouble(2);

            listaProductos.add(new Producto(codigo, descripcion, precio));
        }

        db.close();
        fila.close();

        adaptador = new AdaptadorProducto(listaProductos);

        rvProductos.setAdapter(adaptador);*/
    }

    @SuppressWarnings("deprecation")
    private Bitmap obtenerMiniatura(Intent datos) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return datos.getParcelableExtra("data", Bitmap.class);
        }
        return datos.getParcelableExtra("data");
    }

    private void mostrarSelectorImagen() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Imagen del producto")
                .setItems(new String[]{"Tomar foto con Cámara", "Elegir de Galería"}, (dialog, opcion) -> {
                    if (opcion == 0) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED) {
                            abrirCamaraProducto();
                        } else {
                            permisoCamaraLauncher.launch(Manifest.permission.CAMERA);
                        }
                    } else {
                        Intent galeria = new Intent(Intent.ACTION_GET_CONTENT);
                        galeria.setType("image/*");
                        selectorImagenLauncher.launch(Intent.createChooser(galeria, "Elegir imagen"));
                    }
                })
                .show();
    }

    private void abrirCamaraProducto() {
        Intent camara = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            selectorImagenLauncher.launch(camara);
        } catch (android.content.ActivityNotFoundException error) {
            Toast.makeText(this, "No hay una aplicación de cámara disponible", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap cargarBitmapDesdeUri(Uri uri) {
        try (InputStream streamMedidas = getContentResolver().openInputStream(uri)) {
            if (streamMedidas == null) return null;
            BitmapFactory.Options medidas = new BitmapFactory.Options();
            medidas.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(streamMedidas, null, medidas);

            int muestra = 1;
            while (medidas.outWidth / muestra > 1200 || medidas.outHeight / muestra > 1200) {
                muestra *= 2;
            }

            BitmapFactory.Options opciones = new BitmapFactory.Options();
            opciones.inSampleSize = muestra;
            try (InputStream streamImagen = getContentResolver().openInputStream(uri)) {
                return streamImagen == null ? null : BitmapFactory.decodeStream(streamImagen, null, opciones);
            }
        } catch (IOException | SecurityException error) {
            Log.e("IMAGEN_PRODUCTO", "No se pudo leer la imagen seleccionada", error);
            return null;
        }
    }

    private void cargarYProcesarImagen(Uri uri) {
        btnCrearArticulo.setEnabled(false);
        pbCarga.setVisibility(View.VISIBLE);
        new Thread(() -> {
            Bitmap bitmap = cargarBitmapDesdeUri(uri);
            if (bitmap == null) {
                runOnUiThread(() -> {
                    pbCarga.setVisibility(View.GONE);
                    btnCrearArticulo.setEnabled(true);
                    Toast.makeText(this, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show();
                });
            } else {
                procesarImagenProducto(bitmap);
            }
        }).start();
    }

    private void iniciarProcesamientoImagen(Bitmap bitmap) {
        btnCrearArticulo.setEnabled(false);
        pbCarga.setVisibility(View.VISIBLE);
        new Thread(() -> procesarImagenProducto(bitmap)).start();
    }

    private void procesarImagenProducto(Bitmap bitmapOriginal) {
        try {
            int maxDimension = 350;
            float ratio = Math.min(
                    (float) maxDimension / bitmapOriginal.getWidth(),
                    (float) maxDimension / bitmapOriginal.getHeight()
            );
            int width = Math.max(1, Math.round(ratio * bitmapOriginal.getWidth()));
            int height = Math.max(1, Math.round(ratio * bitmapOriginal.getHeight()));
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmapOriginal, width, height, true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            String base64 = android.util.Base64.encodeToString(
                    baos.toByteArray(), android.util.Base64.NO_WRAP);
            runOnUiThread(() -> {
                imagenBase64 = base64;
                imgProductoPreview.setImageBitmap(scaledBitmap);
                imgProductoPreview.setVisibility(View.VISIBLE);
                pbCarga.setVisibility(View.GONE);
                btnCrearArticulo.setEnabled(true);
            });

            if (scaledBitmap != bitmapOriginal && !bitmapOriginal.isRecycled()) {
                bitmapOriginal.recycle();
            }
        } catch (Exception error) {
            Log.e("IMAGEN_PRODUCTO", "No se pudo procesar la imagen", error);
            runOnUiThread(() -> {
                pbCarga.setVisibility(View.GONE);
                btnCrearArticulo.setEnabled(true);
                Toast.makeText(this, "No se pudo procesar la imagen", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void limpiarImagenProducto() {
        imagenBase64 = "";
        imgProductoPreview.setImageDrawable(null);
        imgProductoPreview.setVisibility(View.GONE);
    }

    private interface CoordenadasProductoCallback {
        void onCoordenadas(double latitud, double longitud);
    }

    private interface ImagenProductoStorageCallback {
        void onResultado(String imagenUrl, Exception error);
    }

    private void subirImagenProductoSiSeleccionada(String codigo, ImagenProductoStorageCallback callback) {
        if (imagenBase64 == null || imagenBase64.trim().isEmpty()) {
            callback.onResultado(null, null);
            return;
        }

        try {
            byte[] bytesImagen = android.util.Base64.decode(imagenBase64, android.util.Base64.DEFAULT);
            com.google.firebase.storage.StorageReference referencia =
                    com.google.firebase.storage.FirebaseStorage.getInstance()
                            .getReference()
                            .child("productos/" + codigo + ".jpg");
            com.google.firebase.storage.StorageMetadata metadata =
                    new com.google.firebase.storage.StorageMetadata.Builder()
                            .setContentType("image/jpeg")
                            .build();

            referencia.putBytes(bytesImagen, metadata)
                    .addOnSuccessListener(tarea -> referencia.getDownloadUrl()
                            .addOnSuccessListener(uri -> callback.onResultado(uri.toString(), null))
                            .addOnFailureListener(error -> callback.onResultado(null, error)))
                    .addOnFailureListener(error -> callback.onResultado(null, error));
        } catch (Exception error) {
            callback.onResultado(null, error);
        }
    }

    private void obtenerCoordenadasProducto(CoordenadasProductoCallback callback) {
        double latitudBoutique = 6.2442;
        double longitudBoutique = -75.5812;
        boolean tienePermisoUbicacion =
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;

        if (!tienePermisoUbicacion) {
            if (permisoUbicacionLauncher == null) {
                callback.onCoordenadas(latitudBoutique, longitudBoutique);
                return;
            }
            coordenadasPendientes = callback;
            permisoUbicacionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }

        try {
            com.google.android.gms.location.FusedLocationProviderClient cliente =
                    com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this);
            com.google.android.gms.tasks.CancellationTokenSource cancelacion =
                    new com.google.android.gms.tasks.CancellationTokenSource();

            cliente.getCurrentLocation(
                            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                            cancelacion.getToken())
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            callback.onCoordenadas(location.getLatitude(), location.getLongitude());
                        } else {
                            obtenerUltimaUbicacionComoRespaldo(cliente, callback,
                                    latitudBoutique, longitudBoutique);
                        }
                    })
                    .addOnFailureListener(error ->
                            obtenerUltimaUbicacionComoRespaldo(cliente, callback,
                                    latitudBoutique, longitudBoutique));
        } catch (SecurityException error) {
            callback.onCoordenadas(latitudBoutique, longitudBoutique);
        }
    }

    private void obtenerUltimaUbicacionComoRespaldo(
            com.google.android.gms.location.FusedLocationProviderClient cliente,
            CoordenadasProductoCallback callback,
            double latitudBoutique,
            double longitudBoutique) {
        try {
            cliente.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            callback.onCoordenadas(location.getLatitude(), location.getLongitude());
                        } else {
                            callback.onCoordenadas(latitudBoutique, longitudBoutique);
                        }
                    })
                    .addOnFailureListener(error ->
                            callback.onCoordenadas(latitudBoutique, longitudBoutique));
        } catch (SecurityException error) {
            callback.onCoordenadas(latitudBoutique, longitudBoutique);
        }
    }

    private void registrarProductoFirebase() {
        try {
            String codigo = etCodigo.getText().toString().trim();
            String descripcion = etDescripcion.getText().toString().trim();
            String precioStr = etPrecio.getText().toString().trim();

            if (codigo.isEmpty() || descripcion.isEmpty() || precioStr.isEmpty()) {
                Toast.makeText(MainActivity.this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            String codigoParaGuardar = codigo.startsWith("PERF-") ? codigo.substring(5) : codigo;
            int codigoNumerico = Integer.parseInt(codigoParaGuardar);
            double precioOriginal = parsePrecioIngresado(precioStr);
            double porcentajeDescuento = obtenerPorcentajeDescuento();
            double precio = calcularPrecioFinal(precioOriginal, porcentajeDescuento);
            boolean estaEnOferta = swOferta.isChecked();
            String imgFinal = imagenBase64 != null ? imagenBase64 : "";
            java.util.List<String> familiasOlfativas = new java.util.ArrayList<>(familiasSeleccionadas);
            String familiaOlfativa = familiasOlfativas.isEmpty() ? "Sin clasificar" : familiasOlfativas.get(0);
            String publicoObjetivo = obtenerSeleccion(spPublicoObjetivo);

            Map<String, Object> productoMap = new HashMap<>();
            productoMap.put("codigo", codigoNumerico);
            productoMap.put("descripcion", descripcion);
            productoMap.put("precio", precio);
            productoMap.put("precioOriginal", precioOriginal);
            productoMap.put("porcentajeDescuento", porcentajeDescuento);
            productoMap.put("oferta", estaEnOferta);
            productoMap.put("enOferta", estaEnOferta);
            productoMap.put("marca", "Scently Exclusive");
            productoMap.put("stock", 1);
            productoMap.put("familiaOlfativa", familiaOlfativa);
            productoMap.put("familiasOlfativas", familiasOlfativas);
            productoMap.put("publicoObjetivo", publicoObjetivo);
            productoMap.put("imagenBase64", imgFinal);

            pbCarga.setVisibility(View.VISIBLE);
            btnCrearArticulo.setEnabled(false);
            buscarDocumentoPorCodigo(codigo, documentoExistente -> {
                obtenerCoordenadasProducto((latitud, longitud) -> {
                    productoMap.put("latitud", latitud);
                    productoMap.put("longitud", longitud);

                    boolean actualizandoExistente = documentoExistente.exists();
                    if (actualizandoExistente) {
                        if (documentoExistente.contains("marca")) productoMap.remove("marca");
                        if (documentoExistente.contains("stock")) productoMap.remove("stock");
                        if (imgFinal.isEmpty()) productoMap.remove("imagenBase64");
                    }

                    subirImagenProductoSiSeleccionada(codigo, (imagenUrl, errorImagen) -> {
                        if (errorImagen != null) {
                            pbCarga.setVisibility(View.GONE);
                            btnCrearArticulo.setEnabled(true);
                            Toast.makeText(MainActivity.this,
                                    "Error al subir la imagen: " + errorImagen.getMessage(), Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (imagenUrl != null) productoMap.put("imagenUrl", imagenUrl);

                        com.google.android.gms.tasks.Task<Void> guardado = actualizandoExistente
                                ? documentoExistente.getReference().update(productoMap)
                                : db.collection("productos").document(codigo).set(productoMap);

                        guardado.addOnSuccessListener(aVoid -> {
                            pbCarga.setVisibility(View.GONE);
                            btnCrearArticulo.setEnabled(true);
                            String mensaje = actualizandoExistente
                                    ? "El producto existente fue actualizado; no se creó otro."
                                    : "¡Producto guardado exitosamente!";
                            Toast.makeText(MainActivity.this, mensaje, Toast.LENGTH_SHORT).show();
                            lanzarNotificacionLocal(
                                    actualizandoExistente ? "Producto actualizado" : "¡Nuevo Producto Registrado!",
                                    "El producto '" + descripcion + "' con código " + codigo + " ya está sincronizado en la nube.");
                            limpiarFormulario();
                        }).addOnFailureListener(error -> {
                            pbCarga.setVisibility(View.GONE);
                            btnCrearArticulo.setEnabled(true);
                            Toast.makeText(MainActivity.this, "Error al guardar: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    });
                });
            });
        } catch (NumberFormatException error) {
            pbCarga.setVisibility(View.GONE);
            btnCrearArticulo.setEnabled(true);
            Toast.makeText(MainActivity.this, "Ingresa un precio numérico válido", Toast.LENGTH_SHORT).show();
        } catch (Exception error) {
            pbCarga.setVisibility(View.GONE);
            btnCrearArticulo.setEnabled(true);
            String mensaje = error.getMessage() == null ? "Error inesperado" : error.getMessage();
            Toast.makeText(MainActivity.this, "Ocurrió un error: " + mensaje, Toast.LENGTH_SHORT).show();
        }
    }

    private void actualizarEstadoVacio() {
        if (llEmptyState != null) {
            boolean listaVacia = listaProductos == null || listaProductos.isEmpty();
            llEmptyState.setVisibility(listaVacia ? View.VISIBLE : View.GONE);
        }
    }

    private void cargarProductosFirebase(){
        listaProductos = new ArrayList<>();

        db.collection("productos").get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()){
                    listaProductos.clear();

                    for (QueryDocumentSnapshot document : task.getResult()){
                        Producto producto = document.toObject(Producto.class);
                        listaProductos.add(producto);
                    }

                    adaptador = new AdaptadorProducto(listaProductos, rolCached);
                    rvProductos.setAdapter(adaptador);
                    actualizarEstadoVacio();
                } else {
                    Toast.makeText(MainActivity.this, "Error al cargar los datos", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void cargarProductosFirebaseTiempoReal(){
        listaProductos = new ArrayList<>();
        catalogoCompleto.clear();
        adaptador = new AdaptadorProducto(listaProductos, rolCached);
        rvProductos.setAdapter(adaptador);
        actualizarEstadoVacio();

        listenerFirestore = db.collection("productos")
            .addSnapshotListener((value, error) -> {
                if (error != null){
                    Toast.makeText(MainActivity.this, "Fallo al escuchar los cambios", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (value != null) {
                    catalogoCompleto.clear();
                    for (QueryDocumentSnapshot documento : value){
                        catalogoCompleto.add(documento.toObject(Producto.class));
                    }
                    actualizarProductosVisibles();
                }
            });
    }

    private void filtrarSoloOfertas() {
        android.widget.LinearLayout contenido = new android.widget.LinearLayout(this);
        contenido.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        contenido.setPadding(padding, padding, padding, padding);

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(contenido);

        TextView familiasElegidas = new TextView(this);
        familiasElegidas.setTextColor(ContextCompat.getColor(this, R.color.scently_primary));
        familiasElegidas.setPadding(0, padding, 0, padding);
        familiasElegidas.setText(familiasFiltro.isEmpty() ? "Todas las familias"
                : android.text.TextUtils.join(", ", familiasFiltro));
        familiasElegidas.setOnClickListener(v -> mostrarSelectorFamilias(familiasFiltro, seleccion -> {
            familiasFiltro.clear();
            familiasFiltro.addAll(seleccion);
            familiasElegidas.setText(familiasFiltro.isEmpty() ? "Todas las familias"
                    : android.text.TextUtils.join(", ", familiasFiltro));
        }));
        TextView etiquetaFamilia = new TextView(this);
        etiquetaFamilia.setText("Familias olfativas (puedes elegir varias)");
        etiquetaFamilia.setTypeface(null, android.graphics.Typeface.BOLD);
        contenido.addView(etiquetaFamilia);
        contenido.addView(familiasElegidas);

        Spinner publico = new Spinner(this);
        String[] publicosFiltro = opcionesFiltro("Todos", PUBLICOS_OBJETIVO);
        configurarSpinnerDialogo(contenido, "Público", publico, publicosFiltro);

        EditText precioMinimo = new EditText(this);
        precioMinimo.setSingleLine(true);
        precioMinimo.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        precioMinimo.setHint("Precio mínimo");
        contenido.addView(precioMinimo);

        EditText precioMaximo = new EditText(this);
        precioMaximo.setSingleLine(true);
        precioMaximo.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        precioMaximo.setHint("Precio máximo");
        contenido.addView(precioMaximo);

        CheckBox soloOfertas = new CheckBox(this);
        soloOfertas.setText("Mostrar solo productos en oferta");
        soloOfertas.setChecked(filtroOfertasActivo);
        contenido.addView(soloOfertas);

        seleccionarSpinner(publico, publicoFiltro);
        if (precioMinimoFiltro != null) precioMinimo.setText(String.valueOf(Math.round(precioMinimoFiltro)));
        if (precioMaximoFiltro != null) precioMaximo.setText(String.valueOf(Math.round(precioMaximoFiltro)));

        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle("Filtrar catálogo")
                .setView(scroll)
                .setPositiveButton("APLICAR", null)
                .setNeutralButton("LIMPIAR", null)
                .setNegativeButton("CANCELAR", null)
                .create();
        dialogo.setOnShowListener(dialog -> {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                try {
                    String minimoTexto = precioMinimo.getText().toString().trim();
                    String maximoTexto = precioMaximo.getText().toString().trim();
                    Double minimo = minimoTexto.isEmpty() ? null : parsePrecioIngresado(minimoTexto);
                    Double maximo = maximoTexto.isEmpty() ? null : parsePrecioIngresado(maximoTexto);
                    if ((minimo != null && minimo < 0) || (maximo != null && maximo < 0)
                            || (minimo != null && maximo != null && minimo > maximo)) {
                        Toast.makeText(this, "Revisa el rango de precios", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    publicoFiltro = String.valueOf(publico.getSelectedItem());
                    precioMinimoFiltro = minimo;
                    precioMaximoFiltro = maximo;
                    filtroOfertasActivo = soloOfertas.isChecked();
                    actualizarProductosVisibles();
                    dialogo.dismiss();
                } catch (NumberFormatException error) {
                    Toast.makeText(this, "Ingresa un rango de precios válido", Toast.LENGTH_SHORT).show();
                }
            });
            dialogo.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                familiasFiltro.clear();
                publicoFiltro = "Todos";
                precioMinimoFiltro = null;
                precioMaximoFiltro = null;
                filtroOfertasActivo = false;
                actualizarProductosVisibles();
                dialogo.dismiss();
            });
        });
        dialogo.show();
    }

    private String[] opcionesFiltro(String todos, String[] opciones) {
        String[] resultado = new String[opciones.length + 1];
        resultado[0] = todos;
        System.arraycopy(opciones, 0, resultado, 1, opciones.length);
        return resultado;
    }

    private void configurarSpinnerDialogo(
            android.widget.LinearLayout contenedor,
            String etiqueta,
            Spinner spinner,
            String[] opciones) {
        TextView label = new TextView(this);
        label.setText(etiqueta);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        contenedor.addView(label);

        android.widget.ArrayAdapter<String> spinnerAdapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, opciones);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        contenedor.addView(spinner);
    }

    private void actualizarProductosVisibles() {
        if (listaProductos == null) return;
        listaProductos.clear();
        for (Producto producto : catalogoCompleto) {
            double precio = producto.getPrecio();
            java.util.List<String> familiasProducto = obtenerFamiliasProducto(producto);
            boolean coincideFamilia = familiasFiltro.isEmpty();
            if (!coincideFamilia) {
                for (String familia : familiasProducto) {
                    if (familiasFiltro.contains(familia)) {
                        coincideFamilia = true;
                        break;
                    }
                }
            }
            boolean coincidePublico = "Todos".equalsIgnoreCase(publicoFiltro)
                    || publicoFiltro.equalsIgnoreCase(producto.getPublicoObjetivo() == null
                    ? "" : producto.getPublicoObjetivo());
            boolean coincideMinimo = precioMinimoFiltro == null || precio >= precioMinimoFiltro;
            boolean coincideMaximo = precioMaximoFiltro == null || precio <= precioMaximoFiltro;
            boolean coincideOferta = !filtroOfertasActivo || producto.isOferta();
            String consulta = normalizarTextoBusqueda(busquedaCatalogo);
            String codigo = String.valueOf(producto.getCodigo());
            String codigoConPrefijo = "perf-" + codigo;
            String descripcion = normalizarTextoBusqueda(producto.getDescripcion());
            String marca = normalizarTextoBusqueda(producto.getMarca());
            boolean coincideBusqueda = consulta.isEmpty()
                    || normalizarTextoBusqueda(codigo).contains(consulta)
                    || normalizarTextoBusqueda(codigoConPrefijo).contains(consulta)
                    || descripcion.contains(consulta)
                    || marca.contains(consulta);
            if (coincideFamilia && coincidePublico && coincideMinimo && coincideMaximo
                    && coincideOferta && coincideBusqueda) {
                listaProductos.add(producto);
            }
        }
        if (adaptador != null) adaptador.notifyDataSetChanged();
        actualizarEstadoVacio();
    }

    private void configurarSeleccionDeProductos(){
        if (!"Administrador".equalsIgnoreCase(rolCached == null ? "" : rolCached.trim())){
            return;
        }

        GestureDetector detector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener(){
                    @Override
                    public boolean onSingleTapUp(@NonNull MotionEvent event){
                        View tarjeta = rvProductos.findChildViewUnder(event.getX(), event.getY());
                        if (tarjeta == null) return false;
                        int posicion = rvProductos.getChildAdapterPosition(tarjeta);
                        if (posicion == RecyclerView.NO_POSITION || listaProductos == null
                                || posicion >= listaProductos.size()) return false;
                        seleccionarProductoParaEdicion(listaProductos.get(posicion));
                        return true;
                    }
                });

        rvProductos.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener(){
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView recyclerView,
                                                 @NonNull MotionEvent event){
                return detector.onTouchEvent(event);
            }
        });
    }

    private void seleccionarProductoParaEdicion(Producto producto){
        etCodigo.setText(String.valueOf(producto.getCodigo()));
        etDescripcion.setText(producto.getDescripcion());
        java.text.DecimalFormat formatoPrecio = new java.text.DecimalFormat("#,##0.##",
                java.text.DecimalFormatSymbols.getInstance(
                        java.util.Locale.forLanguageTag("es-CO")));
        double precioOriginal = producto.getPrecioOriginal() > 0
                ? producto.getPrecioOriginal() : producto.getPrecio();
        etPrecio.setText(formatoPrecio.format(precioOriginal));
        etPorcentajeDescuento.setText(producto.getPorcentajeDescuento() > 0
                ? formatoPrecio.format(producto.getPorcentajeDescuento()) : "");
        familiasSeleccionadas.clear();
        familiasSeleccionadas.addAll(obtenerFamiliasProducto(producto));
        actualizarTextoFamiliasFormulario();
        seleccionarSpinner(spPublicoObjetivo, producto.getPublicoObjetivo());
        swOferta.setChecked(producto.isOferta());
        actualizarVistaPreviaDescuento();

        imagenBase64 = producto.getImagenBase64() == null ? "" : producto.getImagenBase64();
        if (!imagenBase64.trim().isEmpty()){
            try {
                byte[] bytes = android.util.Base64.decode(imagenBase64, android.util.Base64.DEFAULT);
                Bitmap imagen = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (imagen != null){
                    imgProductoPreview.setImageBitmap(imagen);
                    imgProductoPreview.setVisibility(View.VISIBLE);
                } else {
                    limpiarImagenProducto();
                }
            } catch (IllegalArgumentException error){
                limpiarImagenProducto();
            }
        } else {
            limpiarImagenProducto();
        }
        Toast.makeText(this, "Producto cargado en formulario", Toast.LENGTH_SHORT).show();
    }

    private void buscarProductoFirebase(){
        busquedaCatalogo = etBuscarCatalogo.getText().toString().trim();
        actualizarProductosVisibles();

        if (busquedaCatalogo.isEmpty()) {
            Toast.makeText(this, "Búsqueda limpiada", Toast.LENGTH_SHORT).show();
        } else if (listaProductos.isEmpty()) {
            Toast.makeText(this, "No se encontraron fragancias", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Se encontraron " + listaProductos.size() + " fragancias", Toast.LENGTH_SHORT).show();
        }
    }

    private String normalizarTextoBusqueda(String texto) {
        if (texto == null) return "";
        return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(java.util.Locale.ROOT)
                .trim();
    }

    private void cargarProductoEnFormulario(com.google.firebase.firestore.DocumentSnapshot documento) {
        String descripcion = documento.getString("descripcion");
        Object precioObject = documento.get("precio");
        double precio = precioObject instanceof Number ? ((Number) precioObject).doubleValue() : 0d;
        Object precioOriginalObject = documento.get("precioOriginal");
        double precioOriginal = precioOriginalObject instanceof Number
                ? ((Number) precioOriginalObject).doubleValue() : precio;
        Object descuentoObject = documento.get("porcentajeDescuento");
        double porcentajeDescuento = descuentoObject instanceof Number
                ? ((Number) descuentoObject).doubleValue() : 0d;
        Boolean oferta = documento.getBoolean("oferta");
        if (oferta == null) oferta = documento.getBoolean("enOferta");

        etDescripcion.setText(descripcion == null ? "" : descripcion);
        java.text.DecimalFormat formatoPrecio = new java.text.DecimalFormat("#,##0.##",
                java.text.DecimalFormatSymbols.getInstance(java.util.Locale.forLanguageTag("es-CO")));
        etPrecio.setText(formatoPrecio.format(precioOriginal > 0 ? precioOriginal : precio));
        etPorcentajeDescuento.setText(porcentajeDescuento > 0
                ? formatoPrecio.format(porcentajeDescuento) : "");
        familiasSeleccionadas.clear();
        familiasSeleccionadas.addAll(obtenerFamiliasDocumento(documento));
        actualizarTextoFamiliasFormulario();
        seleccionarSpinner(spPublicoObjetivo, documento.getString("publicoObjetivo"));
        swOferta.setChecked(Boolean.TRUE.equals(oferta));
        actualizarVistaPreviaDescuento();

        Object imagenObject = documento.get("imagenBase64");
        imagenBase64 = imagenObject instanceof String ? (String) imagenObject : "";
        if (!imagenBase64.trim().isEmpty()) {
            try {
                byte[] bytes = android.util.Base64.decode(imagenBase64, android.util.Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap != null) {
                    imgProductoPreview.setImageBitmap(bitmap);
                    imgProductoPreview.setVisibility(View.VISIBLE);
                } else {
                    limpiarImagenProducto();
                }
            } catch (IllegalArgumentException error) {
                limpiarImagenProducto();
            }
        } else {
            limpiarImagenProducto();
        }
    }

    private void actualizarProductoFirebase(){
        if (!"Administrador".equalsIgnoreCase(rolCached == null ? "" : rolCached.trim())) {
            Toast.makeText(this, "No tienes permisos para editar productos", Toast.LENGTH_SHORT).show();
            return;
        }
        String codigo = etCodigo.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String precioTexto = etPrecio.getText().toString().trim();
        if (codigo.isEmpty() || descripcion.isEmpty() || precioTexto.isEmpty()) {
            Toast.makeText(this, "Llena todos los campos del formulario", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double precioOriginal = parsePrecioIngresado(precioTexto);
            double porcentajeDescuento = obtenerPorcentajeDescuento();
            double precio = calcularPrecioFinal(precioOriginal, porcentajeDescuento);
            Map<String, Object> actualizaciones = new HashMap<>();
            actualizaciones.put("descripcion", descripcion);
            actualizaciones.put("precio", precio);
            actualizaciones.put("precioOriginal", precioOriginal);
            actualizaciones.put("porcentajeDescuento", porcentajeDescuento);
            actualizaciones.put("oferta", swOferta.isChecked());
            actualizaciones.put("enOferta", swOferta.isChecked());
            java.util.List<String> familiasOlfativas = new java.util.ArrayList<>(familiasSeleccionadas);
            actualizaciones.put("familiasOlfativas", familiasOlfativas);
            actualizaciones.put("familiaOlfativa", familiasOlfativas.isEmpty()
                    ? "Sin clasificar" : familiasOlfativas.get(0));
            actualizaciones.put("publicoObjetivo", obtenerSeleccion(spPublicoObjetivo));
            if (imagenBase64 != null && !imagenBase64.isEmpty()) {
                actualizaciones.put("imagenBase64", imagenBase64);
            }
            buscarDocumentoPorCodigo(codigo, documento -> {
                if (!documento.exists()) {
                    Toast.makeText(this, "Código no encontrado para editar", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!documento.contains("marca")) actualizaciones.put("marca", "Scently Exclusive");
                if (!documento.contains("stock")) actualizaciones.put("stock", 1);
                obtenerCoordenadasProducto((latitud, longitud) -> {
                    actualizaciones.put("latitud", latitud);
                    actualizaciones.put("longitud", longitud);
                    subirImagenProductoSiSeleccionada(codigo, (imagenUrl, errorImagen) -> {
                        if (errorImagen != null) {
                            Toast.makeText(this, "Error al subir la imagen: " + errorImagen.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (imagenUrl != null) actualizaciones.put("imagenUrl", imagenUrl);
                        documento.getReference().update(actualizaciones)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "¡Producto actualizado en Firestore!", Toast.LENGTH_SHORT).show();
                                    lanzarNotificacionLocal("Scently Catálogo",
                                            "El producto " + codigo + " ha sido actualizado con éxito.");
                                    limpiarFormulario();
                                })
                                .addOnFailureListener(error ->
                                        Toast.makeText(this, "Error al actualizar: " + error.getMessage(), Toast.LENGTH_LONG).show());
                    });
                });
            });
        } catch (NumberFormatException error) {
            Toast.makeText(this, "Revisa el precio y el descuento ingresados", Toast.LENGTH_SHORT).show();
        }
    }

    private void eliminarProductoFirebase(){
        if (!"Administrador".equalsIgnoreCase(rolCached == null ? "" : rolCached.trim())) {
            Toast.makeText(this, "No tienes permisos para eliminar productos", Toast.LENGTH_SHORT).show();
            return;
        }
        String codigo = etCodigo.getText().toString().trim();
        if (codigo.isEmpty()) {
            Toast.makeText(this, "Ingrese el codigo del producto a eliminar", Toast.LENGTH_SHORT).show();
            return;
        }

        buscarDocumentoPorCodigo(codigo, documento -> {
            if (!documento.exists()) {
                Toast.makeText(this, "No existe producto con ese código", Toast.LENGTH_SHORT).show();
                return;
            }
            documento.getReference().delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "¡Producto eliminado permanentemente!", Toast.LENGTH_SHORT).show();
                        limpiarFormulario();
                        // La escucha en tiempo real elimina el producto del RecyclerView.
                    })
                    .addOnFailureListener(error ->
                            Toast.makeText(this, "Error al borrar el producto: " + error.getMessage(), Toast.LENGTH_LONG).show());
        });
    }

    private void buscarDocumentoPorCodigo(
            String codigo,
            com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.firestore.DocumentSnapshot> alEncontrar) {
        Object codigoNumerico;
        try {
            codigoNumerico = Integer.parseInt(codigo);
        } catch (NumberFormatException error) {
            codigoNumerico = codigo;
        }
        db.collection("productos").whereEqualTo("codigo", codigoNumerico).limit(1).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        alEncontrar.onSuccess(snapshot.getDocuments().get(0));
                    } else {
                        db.collection("productos").document(codigo).get()
                                .addOnSuccessListener(alEncontrar)
                                .addOnFailureListener(error ->
                                        Toast.makeText(this, "Error al consultar Firestore: " + error.getMessage(), Toast.LENGTH_LONG).show());
                    }
                })
                .addOnFailureListener(error ->
                        Toast.makeText(this, "Error al consultar Firestore: " + error.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void limpiarFormulario() {
        etCodigo.setText("");
        etDescripcion.setText("");
        etPrecio.setText("");
        etPorcentajeDescuento.setText("");
        familiasSeleccionadas.clear();
        actualizarTextoFamiliasFormulario();
        seleccionarSpinner(spPublicoObjetivo, "Sin clasificar");
        tilCodigo.setErrorEnabled(false);
        tilDescripcion.setErrorEnabled(false);
        tilPrecio.setErrorEnabled(false);
        tilPorcentajeDescuento.setErrorEnabled(false);
        swOferta.setChecked(false);
        tvPrecioCalculado.setVisibility(View.GONE);
        limpiarImagenProducto();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (listenerFirestore != null) {
            listenerFirestore.remove();
        }
    }

    private interface DummyJsonApi {
        @retrofit2.http.GET("products/category/fragrances")
        Call<com.google.gson.JsonObject> listarFragancias();
    }

    private void obtenerProductosAPI(){
        btnAutocompletarAPI.setEnabled(false);
        pbCargaApi.setVisibility(View.VISIBLE);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://dummyjson.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        DummyJsonApi api = retrofit.create(DummyJsonApi.class);
        api.listarFragancias().enqueue(new Callback<com.google.gson.JsonObject>() {
            @Override
            public void onResponse(Call<com.google.gson.JsonObject> call,
                                   Response<com.google.gson.JsonObject> response) {
                btnAutocompletarAPI.setEnabled(true);
                pbCargaApi.setVisibility(View.GONE);

                if (!response.isSuccessful() || response.body() == null
                        || !response.body().has("products")
                        || !response.body().get("products").isJsonArray()) {
                    Toast.makeText(MainActivity.this, "No se pudieron cargar fragancias desde la API", Toast.LENGTH_SHORT).show();
                    return;
                }

                com.google.gson.JsonArray productos = response.body().getAsJsonArray("products");
                if (productos.size() == 0) {
                    Toast.makeText(MainActivity.this, "La API no devolvió fragancias", Toast.LENGTH_SHORT).show();
                    return;
                }

                java.util.Random random = new java.util.Random();
                com.google.gson.JsonObject fragancia = productos.get(random.nextInt(productos.size())).getAsJsonObject();
                String tituloPerfume = fragancia.has("title") && !fragancia.get("title").isJsonNull()
                        ? fragancia.get("title").getAsString() : "Fragancia Scently";
                double precioUsd = fragancia.has("price") && fragancia.get("price").isJsonPrimitive()
                        && fragancia.get("price").getAsJsonPrimitive().isNumber()
                        ? fragancia.get("price").getAsDouble() : 0d;

                String codigoGenerado = "PERF-" + (100 + random.nextInt(900));
                long precioEnPesos = Math.round(precioUsd * 4000d);
                etCodigo.setText(codigoGenerado);
                etDescripcion.setText(tituloPerfume);
                etPrecio.setText(String.valueOf(precioEnPesos));
                Toast.makeText(MainActivity.this,
                        "Fragancia sugerida desde la API: " + tituloPerfume,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<com.google.gson.JsonObject> call, Throwable error) {
                btnAutocompletarAPI.setEnabled(true);
                pbCargaApi.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this,
                        "Error de conexión con la API de fragancias",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void aplicarPermisosDeRol(String rol){
        boolean esAdministrador = "Administrador".equalsIgnoreCase(rol == null ? "" : rol.trim());

        if (esAdministrador) {
            cardFormularioProducto.setVisibility(View.VISIBLE);
            btnAdminUsuarios.setVisibility(View.VISIBLE);
            btnEditar.setVisibility(View.VISIBLE);
            btnEditar.setEnabled(true);
            btnBorrar.setVisibility(View.VISIBLE);
            ((View) btnAutocompletarAPI.getParent()).setVisibility(View.VISIBLE);
            btnAutocompletarAPI.setVisibility(View.VISIBLE);
            toolbar.setVisibility(View.VISIBLE);
            toolbar.setTitle("ADMIN");
            toolbar.setSubtitle(null);
            toolbar.setTitleTextAppearance(this, R.style.TextAppearance_Scently_AdminHeader);
            toolbar.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_admin_header));

            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.scently_primary_dark));
        } else {
            cardFormularioProducto.setVisibility(View.GONE);
            btnAdminUsuarios.setVisibility(View.GONE);
            btnEditar.setVisibility(View.GONE);
            btnBorrar.setVisibility(View.GONE);
            ((View) btnAutocompletarAPI.getParent()).setVisibility(View.GONE);
            pbCargaApi.setVisibility(View.GONE);

            toolbar.setVisibility(View.VISIBLE);
            toolbar.setTitle("¡Hola, " + obtenerNombreEmpleado() + "!");
            toolbar.setSubtitle("Catálogo de Consulta · Asesor");
            toolbar.setTitleTextAppearance(this, R.style.TextAppearance_Scently_EmployeeHeader);
            toolbar.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_employee_header));

            btnFiltrar.setVisibility(View.VISIBLE);
            btnBuscar.setVisibility(View.VISIBLE);
            btnAbrirMapa.setVisibility(View.VISIBLE);
            btnCerrarSesion.setVisibility(View.VISIBLE);

            if (etNombreTienda != null && etNombreTienda.getParent() instanceof View) {
                ((View) etNombreTienda.getParent()).setVisibility(View.GONE);
            }
        }
    }

    private String obtenerNombreEmpleado() {
        String nombre = nombreCached == null ? "" : nombreCached.trim();
        if (nombre.isEmpty() || "Usuario".equalsIgnoreCase(nombre)) {
            String email = mAuth.getCurrentUser() == null ? null : mAuth.getCurrentUser().getEmail();
            if (email != null && !email.trim().isEmpty()) {
                int separador = email.indexOf('@');
                nombre = separador > 0 ? email.substring(0, separador) : email;
            } else {
                nombre = "Empleado";
            }
        }

        String primerNombre = nombre.split("\\s+")[0];
        return primerNombre.substring(0, 1).toUpperCase(java.util.Locale.ROOT)
                + primerNombre.substring(1);
    }

    private void crearCanalDeNotificaciones(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence nombreCanal = "Alertas de Inventario";
            String descripcion = "Notificaciones sobre altas, bajas y cambios en los Productos";
            int importancia = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel canal = new NotificationChannel(CANAL_ID, nombreCanal, importancia);
            canal.setDescription(descripcion);

            NotificationManager manager = getSystemService(NotificationManager.class);

            if (manager != null){
                manager.createNotificationChannel(canal);
            }
        }
    }

    private void verificarPermisoNotificacion(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    101
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && (grantResults.length == 0
                || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(this, "Alertas desactivadas por el usuario", Toast.LENGTH_SHORT).show();
        }
    }

    private void lanzarNotificacionLocal(String titulo, String mensaje){
        Intent destinoIntent = new Intent(this, MainActivity.class);
        destinoIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                destinoIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CANAL_ID)
                .setSmallIcon(R.drawable.ic_caja)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(mensaje))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED){
                int idNotificacion = (int) System.currentTimeMillis();
                managerCompat.notify(idNotificacion, builder.build());
            }
        } else {
            int idNotificacion = (int) System.currentTimeMillis();
            managerCompat.notify(idNotificacion, builder.build());
        }

    }
}