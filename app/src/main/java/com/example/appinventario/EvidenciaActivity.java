package com.example.appinventario;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.security.Permission;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

public class EvidenciaActivity extends AppCompatActivity {

    private ImageView ivPreviaFoto;
    private Button btnTomarFoto, btnAbrirGaleria, btnSubirNube, btnSeleccionarMultiples;
    private ActivityResultLauncher<String> solicitarPermisoCamaraLauncher;
    private ActivityResultLauncher<Void> abrirCamaraLauncher;
    private ActivityResultLauncher<String> abrirGaleriaLauncher;

    private ProgressBar pbSubida;
    private TextView tvPorcentaje;
    private Uri uriFotoGaleria = null;
    private Bitmap bitmapFotoCamara = null;

    private FirebaseStorage storage;
    private StorageReference storageRef;

    private List<Uri> listaUrisSeleccionadas = new ArrayList<>();
    private TextView tvConteoFotos;
    private ActivityResultLauncher<String> abrirGaleriaMultipleLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evidencia);

        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        tvConteoFotos = findViewById(R.id.tvConteoFotos);

        pbSubida = findViewById(R.id.pbSubida);
        tvPorcentaje = findViewById(R.id.tvPorcentaje);
        btnSubirNube = findViewById(R.id.btnSubirNube);

        ivPreviaFoto = findViewById(R.id.ivPreviaFoto);
        btnTomarFoto = findViewById(R.id.btnTomarFoto);
        btnAbrirGaleria = findViewById(R.id.btnAbrirGaleria);
        btnSeleccionarMultiples = findViewById(R.id.btnSeleccionarMultiples);

        abrirGaleriaLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uriResult -> {
                    if (uriResult != null){
                        uriFotoGaleria = uriResult;
                        bitmapFotoCamara = null;
                        ivPreviaFoto.setImageURI(uriResult);
                        Toast.makeText(this, "Imagen cargada desde Galeria", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        abrirCamaraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                bitmapResult -> {
                    if (bitmapResult != null){
                        bitmapFotoCamara = bitmapResult;
                        uriFotoGaleria = null;
                        ivPreviaFoto.setImageBitmap(bitmapResult);
                        Toast.makeText(this, "Foto capturada con exito", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        solicitarPermisoCamaraLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted){
                        abrirCamaraLauncher.launch(null);
                    } else {
                        Toast.makeText(this, "Permiso denegado. No se puede usar la camara.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        btnAbrirGaleria.setOnClickListener(v -> {
            abrirGaleriaLauncher.launch("image/*");
        });

        btnTomarFoto.setOnClickListener(v -> {
            verificarYEjecutarCamara();
        });

        btnSubirNube.setOnClickListener(v -> {
            prepararYSubirImagen();
            subirLoteDeImagenes();
        });

        abrirGaleriaMultipleLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            urisResult -> {
                if (urisResult != null && !urisResult.isEmpty()){
                    listaUrisSeleccionadas.clear();
                    listaUrisSeleccionadas.addAll(urisResult);

                    tvConteoFotos.setText(listaUrisSeleccionadas.size() + " fotos seleccionadas");

                    ivPreviaFoto.setImageURI(listaUrisSeleccionadas.get(0));

                    Toast.makeText(this, "Seleccionaste: " + listaUrisSeleccionadas.size() + " imagenes", Toast.LENGTH_SHORT).show();
                }
            }
        );

        btnSeleccionarMultiples.setOnClickListener(v -> {
            abrirGaleriaMultipleLauncher.launch("image/*");
        });
    }

    private void verificarYEjecutarCamara(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            abrirCamaraLauncher.launch(null);
        } else {
            solicitarPermisoCamaraLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void prepararYSubirImagen(){
        if (uriFotoGaleria == null && bitmapFotoCamara == null){
            Toast.makeText(this, "Primero toma una foto o eligela de la galeria", Toast.LENGTH_SHORT).show();
            return;
        }

        //foto_1695849204912.jpg
        String nombreArchivo = "foto_" + System.currentTimeMillis() + ".jpg";
        StorageReference fotoRef = storageRef.child("evidencias/" + nombreArchivo);

        pbSubida.setVisibility(View.VISIBLE);
        tvPorcentaje.setVisibility(View.VISIBLE);
        pbSubida.setProgress(0);
        btnSubirNube.setEnabled(false);

        UploadTask tareaSubida;

        if (uriFotoGaleria != null){
            tareaSubida = fotoRef.putFile(uriFotoGaleria);
        } else {
            ByteArrayOutputStream base = new ByteArrayOutputStream();
            bitmapFotoCamara.compress(Bitmap.CompressFormat.JPEG, 90, base);
            byte[] datosBytes = base.toByteArray();

            tareaSubida = fotoRef.putBytes(datosBytes);
        }

        tareaSubida.addOnProgressListener(snapshot -> {
           double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
           pbSubida.setProgress((int) progress);
           tvPorcentaje.setText("Subiendo: " + (int) progress + "%");
        });

        tareaSubida.addOnSuccessListener(takeSnapshot -> {
            fotoRef.getDownloadUrl().addOnSuccessListener(uriDescarga -> {

                String urlFinal = uriDescarga.toString();
                String codigoDocumento = "1001";

                guardarEnlaceEnFirestore(codigoDocumento, urlFinal);

                android.util.Log.d("FIREBASE_STORAGE", "URL publica: " + urlFinal);

            });
        }).addOnFailureListener(e -> {
            pbSubida.setVisibility(View.GONE);
            tvPorcentaje.setVisibility(View.GONE);
            btnSubirNube.setEnabled(true);
            Toast.makeText(EvidenciaActivity.this, "Fallo al subir: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void subirLoteDeImagenes(){
        if (listaUrisSeleccionadas.isEmpty()){
            Toast.makeText(this, "No hay fotos seleccionadas para subir", Toast.LENGTH_SHORT).show();
            return;
        }

        pbSubida.setVisibility(View.VISIBLE);
        tvPorcentaje.setVisibility(View.VISIBLE);
        tvPorcentaje.setText("Subiendo lote de " + listaUrisSeleccionadas.size() + " fotos...");
        btnSubirNube.setEnabled(false);

        List<Task<Uri>> tareaDescargaUrls = new ArrayList<>();

        for (int i = 0; i < listaUrisSeleccionadas.size(); i++){
            Uri uriActual = listaUrisSeleccionadas.get(i);

            String nombreArchivo = "lote_" + System.currentTimeMillis() + "_" + i + ".jpg";
            StorageReference archivoRef = storageRef.child("evidencias_lote/" + nombreArchivo);

            UploadTask subida = archivoRef.putFile(uriActual);

            Task<Uri> obtenerUrlTask = subida.continueWithTask(task -> {
                if (!task.isSuccessful()){
                    throw task.getException();
                }

                return archivoRef.getDownloadUrl();
            });

            tareaDescargaUrls.add(obtenerUrlTask);
        }

        Tasks.whenAllSuccess(tareaDescargaUrls)
                .addOnSuccessListener(objetosUrls -> {
                    pbSubida.setVisibility(View.GONE);
                    tvPorcentaje.setVisibility(View.GONE);
                    btnSubirNube.setEnabled(true);

                    List<String> urlsFinales = new ArrayList<>();
                    for (Object obj : objetosUrls){
                        Uri urlConvertida = (Uri) obj;
                        urlsFinales.add(urlConvertida.toString());
                    }

                    Toast.makeText(EvidenciaActivity.this, "Se subieron las " + urlsFinales.size() + " fotos con exito!", Toast.LENGTH_SHORT).show();

                    for (String link : urlsFinales){
                        android.util.Log.d("URLS_FIREBASE", "Foto disponible en: " + link);
                    }

                    listaUrisSeleccionadas.clear();
                    tvConteoFotos.setText("0 fotos seleccionadas");

                }).addOnFailureListener(e -> {
                    pbSubida.setVisibility(View.GONE);
                    tvPorcentaje.setVisibility(View.GONE);
                    btnSubirNube.setEnabled(true);
                    Toast.makeText(EvidenciaActivity.this, "Ocurrio un error en la subida " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void guardarEnlaceEnFirestore(String idDocumento, String urlDescarga) {
        // Referencia al documento en la colección 'productos'
        FirebaseFirestore.getInstance()
                .collection("productos")
                .document(idDocumento)
                .update("urlFoto", urlDescarga)
                .addOnSuccessListener(aVoid -> {
                    // Éxito: Ocultamos loaders y volvemos a la pantalla principal
                    pbSubida.setVisibility(View.GONE);
                    btnSubirNube.setEnabled(true);
                    Toast.makeText(EvidenciaActivity.this, "¡Evidencia fotográfica vinculada con éxito!", Toast.LENGTH_SHORT).show();

                    finish();
                })
                .addOnFailureListener(e -> {
                    pbSubida.setVisibility(View.GONE);
                    btnSubirNube.setEnabled(true);
                    Toast.makeText(EvidenciaActivity.this, "Error al actualizar Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

}