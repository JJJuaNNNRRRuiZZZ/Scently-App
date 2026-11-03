package com.example.appinventario;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.List;
import java.util.ArrayList;

public class GestionUsuariosActivity extends AppCompatActivity {

    private Button btnVolverAdmin;
    private RecyclerView rvListaUsuarios;
    private AdaptadorUsuario adaptador;
    private FirebaseFirestore db;
    private List<Usuario> listaUsuarios;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestion_usuarios);

        btnVolverAdmin = findViewById(R.id.btnVolverAdmin);
        rvListaUsuarios = findViewById(R.id.rvListaUsuarios);

        db = FirebaseFirestore.getInstance();

        btnVolverAdmin.setOnClickListener(v -> finish());

        rvListaUsuarios.setLayoutManager(new LinearLayoutManager(this));
        listaUsuarios = new ArrayList<>();
        adaptador = new AdaptadorUsuario(listaUsuarios, this);
        rvListaUsuarios.setAdapter(adaptador);

        cargarUsuariosRealTime();
    }

    private void cargarUsuariosRealTime() {
        db.collection("usuarios").addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(GestionUsuariosActivity.this, "Error al cargar usuarios", Toast.LENGTH_SHORT).show();
                return;
            }

            if(value != null) {
                listaUsuarios.clear();

                for (QueryDocumentSnapshot documento : value) {
                    Usuario usuario = documento.toObject(Usuario.class);
                    usuario.setIdDocumento(documento.getId());
                    listaUsuarios.add(usuario);
                }

                adaptador.notifyDataSetChanged();
            }
        });
    }
}