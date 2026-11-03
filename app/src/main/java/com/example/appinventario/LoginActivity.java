package com.example.appinventario;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import android.content.SharedPreferences;

public class LoginActivity extends AppCompatActivity {

    private TextView tvIrRegistro;
    private Button btnLogin;
    private com.google.android.material.textfield.TextInputEditText etLoginCorreo, etLoginPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tvIrRegistro = findViewById(R.id.tvIrRegistro);
        btnLogin = findViewById(R.id.btnLogin);
        etLoginCorreo = findViewById(R.id.etLoginCorreo);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null){
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        tvIrRegistro.setOnClickListener(v -> {
            Intent intencion = new Intent(LoginActivity.this, RegistroActivity.class);
            startActivity(intencion);
        });

        btnLogin.setOnClickListener(v -> {
            iniciarSesion();
        });
    }

    private void iniciarSesion(){
        String correo = etLoginCorreo.getText().toString().trim();
        String password = etLoginPassword.getText().toString().trim();

        if (correo.isEmpty() || password.isEmpty()){
            Toast.makeText(LoginActivity.this, "Ingrese correo y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("VERIFICANDO...");

        mAuth.signInWithEmailAndPassword(correo, password)
                .addOnCompleteListener(task -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("INICIAR SESION");

                    if (task.isSuccessful()){

                        String uid = mAuth.getCurrentUser().getUid();

                        db.collection("usuarios").document(uid).get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if(documentSnapshot.exists()){
                                    String nombreReal = documentSnapshot.getString("nombre");
                                    String rol = documentSnapshot.getString("rol");

                                    SharedPreferences prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE);
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putString("nombre", nombreReal);
                                    editor.putString("rol", rol);
                                    editor.apply();

                                    Intent intencion = new Intent(LoginActivity.this, MainActivity.class);
                                    startActivity(intencion);
                                    finish();

                                }
                            });
                    } else {
                        Toast.makeText(LoginActivity.this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}