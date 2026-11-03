package com.example.appinventario;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class AdaptadorProducto extends RecyclerView.Adapter<AdaptadorProducto.ProductoViewHolder> {

    private List<Producto> listaProductos;
    private String rolUsuario;

    public AdaptadorProducto(List<Producto> listaProductos, String rolUsuario) {
        this.listaProductos = listaProductos;
        this.rolUsuario = rolUsuario;
    }

    @NonNull
    @Override
    public ProductoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_producto, parent, false);
        return new ProductoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductoViewHolder holder, int position) {
        Producto productoActual = listaProductos.get(position);

        // Datos del perfume
        holder.tvCodigo.setText("#" + productoActual.getCodigo());
        holder.tvDescripcion.setText(productoActual.getDescripcion());

        // Marca o diseñador
        if (holder.tvMarca != null) {
            String marca = productoActual.getMarca();
            holder.tvMarca.setText((marca != null && !marca.isEmpty()) ? marca : "Scently Exclusive");
        }

        DecimalFormatSymbols simbolos = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("es-CO"));
        DecimalFormat formatoMoneda = new DecimalFormat("$ #,##0", simbolos);
        holder.tvPrecio.setText(formatoMoneda.format(productoActual.getPrecio()));

        double precioOriginal = productoActual.getPrecioOriginal();
        if (productoActual.isOferta() && precioOriginal > productoActual.getPrecio()
                && holder.tvPrecioOriginal != null) {
            holder.tvPrecioOriginal.setText(formatoMoneda.format(precioOriginal));
            holder.tvPrecioOriginal.setPaintFlags(
                    holder.tvPrecioOriginal.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvPrecioOriginal.setVisibility(View.VISIBLE);
        } else if (holder.tvPrecioOriginal != null) {
            holder.tvPrecioOriginal.setVisibility(View.GONE);
        }

        // El badge de disponibilidad solo está presente en el diseño horizontal.
        if (holder.tvEstado != null) {
            if (productoActual.isOferta()) {
                holder.tvEstado.setText("OFERTA");
                holder.tvEstado.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.scently_terracotta));
            } else if (productoActual.getStock() <= 0 && productoActual.getStock() != -1) {
                holder.tvEstado.setText("AGOTADO");
                holder.tvEstado.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.scently_error));
            } else {
                holder.tvEstado.setText("EN STOCK");
                holder.tvEstado.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.scently_accent));
            }
        }

        // Restablece la imagen para evitar mostrar contenido reciclado de otra tarjeta.
        Glide.with(holder.itemView.getContext()).clear(holder.ivItemFoto);
        holder.ivItemFoto.setImageResource(R.drawable.ic_caja);
        holder.ivItemFoto.setScaleType(ImageView.ScaleType.CENTER_CROP);

        String imagenUrl = productoActual.getImagenUrl();
        boolean imagenRemotaCargada = imagenUrl != null && !imagenUrl.trim().isEmpty();
        if (imagenRemotaCargada) {
            Glide.with(holder.itemView.getContext())
                    .load(imagenUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_caja)
                    .error(R.drawable.ic_caja)
                    .transition(DrawableTransitionOptions.withCrossFade(250))
                    .into(holder.ivItemFoto);
        } else {
            String imagenBase64 = productoActual.getImagenBase64();
            boolean imagenBase64Cargada = false;
            if (imagenBase64 != null && !imagenBase64.trim().isEmpty()) {
                try {
                    byte[] decodedBytes = Base64.decode(imagenBase64, Base64.DEFAULT);
                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(
                            decodedBytes, 0, decodedBytes.length);
                    if (bitmap != null) {
                        holder.ivItemFoto.setImageBitmap(bitmap);
                        imagenBase64Cargada = true;
                    }
                } catch (Exception ignored) {
                    // Se mantiene el placeholder si la cadena no contiene una imagen válida.
                }
            }

            // Compatibilidad con URLs y fotos de evidencia de productos antiguos.
            if (!imagenBase64Cargada) {
                Object imagenCarga = null;
                List<String> fotos = productoActual.getFotosEvidencia();
                if (fotos != null && !fotos.isEmpty()
                        && fotos.get(0) != null && !fotos.get(0).isEmpty()) {
                    imagenCarga = fotos.get(0);
                } else if (productoActual.getUrlFoto() != null
                        && !productoActual.getUrlFoto().isEmpty()) {
                    imagenCarga = productoActual.getUrlFoto();
                }

                if (imagenCarga != null) {
                    Glide.with(holder.itemView.getContext())
                            .load(imagenCarga)
                            .centerCrop()
                            .placeholder(R.drawable.ic_caja)
                            .error(R.drawable.ic_caja)
                            .transition(DrawableTransitionOptions.withCrossFade(250))
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                            Target<Drawable> target, boolean isFirstResource) {
                                    Log.e("GLIDE_SCENTLY",
                                            "Fallo al cargar imagen de " + productoActual.getDescripcion());
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model,
                                                               Target<Drawable> target, DataSource dataSource,
                                                               boolean isFirstResource) {
                                    return false;
                                }
                            })
                            .into(holder.ivItemFoto);
                }
            }
        }

        // Control de permisos según Rol (RBAC)
        if ("Vendedor".equalsIgnoreCase(rolUsuario) || "Operario".equalsIgnoreCase(rolUsuario)) {
            holder.itemView.setClickable(false);
            holder.itemView.setFocusable(false);
        } else {
            holder.itemView.setClickable(true);
            holder.itemView.setOnClickListener(v -> {
                Toast.makeText(v.getContext(), "Fragancia seleccionada: " + productoActual.getDescripcion(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public int getItemCount() {
        return listaProductos != null ? listaProductos.size() : 0;
    }

    public static class ProductoViewHolder extends RecyclerView.ViewHolder {
        TextView tvCodigo, tvDescripcion, tvMarca, tvPrecioOriginal, tvPrecio, tvEstado;
        ImageView ivItemFoto;

        public ProductoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCodigo = itemView.findViewById(R.id.tvItemCodigo);
            tvDescripcion = itemView.findViewById(R.id.tvItemDescripcion);
            tvMarca = itemView.findViewById(R.id.tvItemMarca);
            tvPrecioOriginal = itemView.findViewById(R.id.tvItemPrecioOriginal);
            tvPrecio = itemView.findViewById(R.id.tvItemPrecio);
            tvEstado = itemView.findViewById(R.id.tvItemEstado);
            ivItemFoto = itemView.findViewById(R.id.ivItemFoto);
        }
    }
}