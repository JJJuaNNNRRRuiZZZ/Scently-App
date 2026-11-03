package com.example.appinventario;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class AdaptadorUsuario extends RecyclerView.Adapter<AdaptadorUsuario.UsuarioViewHolder> {
    private List<Usuario> listaUsuarios;
    private Context context;
    private FirebaseFirestore db;

    public AdaptadorUsuario(List<Usuario> listaUsuarios, Context context) {
       this.listaUsuarios = listaUsuarios;
       this.context = context;
       this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public UsuarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_usuario, parent, false);
        return new UsuarioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UsuarioViewHolder holder, int position) {
        Usuario usuario = listaUsuarios.get(position);

        holder.tvNombre.setText(usuario.getNombre());
        holder.tvCorreo.setText(usuario.getCorreo());
        holder.tvRol.setText("ROL: " +usuario.getRol());

        holder.btnAlternarRol.setOnClickListener(v -> {
            String nuevoRol = usuario.getRol().equals("Vendedor") ? "Administrador" : "Vendedor";

            db.collection("usuarios").document(usuario.getIdDocumento())
                    .update("rol", nuevoRol)
                    .addOnSuccessListener(aVoid -> Toast.makeText(context, "Rol actualizado a " + nuevoRol, Toast.LENGTH_SHORT).show());
        });

        holder.btnEliminarUsuario.setOnClickListener(v -> {
            db.collection("usuarios").document(usuario.getIdDocumento())
                    .delete()
                    .addOnSuccessListener(aVoid -> Toast.makeText(context, "Usuario eliminado", Toast.LENGTH_SHORT).show());
        });
    }

    @Override
    public int getItemCount() {
        return listaUsuarios.size();
    }

    public static class UsuarioViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvCorreo, tvRol;
        MaterialButton btnAlternarRol, btnEliminarUsuario;

        public UsuarioViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvUsuarioNombre);
            tvCorreo = itemView.findViewById(R.id.tvUsuarioCorreo);
            tvRol = itemView.findViewById(R.id.tvUsuarioRol);
            btnAlternarRol = itemView.findViewById(R.id.btnAlternarRol);
            btnEliminarUsuario = itemView.findViewById(R.id.btnEliminarUsuario);
        }
    }
}
