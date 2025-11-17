package com.example.turistapp_v1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;

public class LugarAdapter extends FirestoreRecyclerAdapter<Lugar, LugarAdapter.LugarViewHolder> {

    private OnLugarClickListener listener;


    public LugarAdapter(@NonNull FirestoreRecyclerOptions<Lugar> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull LugarViewHolder holder, int position, @NonNull Lugar model) {
        // --- 1. Cargar Datos en las Vistas ---

        holder.tvNombre.setText(model.getNombre());
        holder.tvDescripcion.setText(model.getDireccion());

        if (model.getRatingPromedio() > 0) {
            holder.tvRatingValue.setText(String.format("%.1f", model.getRatingPromedio()));
            holder.layoutRating.setVisibility(View.VISIBLE);
        } else {
            holder.layoutRating.setVisibility(View.GONE);
        }

        if (model.getUrlImagen() != null && !model.getUrlImagen().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(model.getUrlImagen())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(holder.ivImagen);
        } else {
            holder.ivImagen.setImageResource(R.drawable.placeholder_image);
        }

        // --- NUEVO: Actualizar el icono de favorito al cargar el ViewHolder ---
        // Este método necesita ser implementado por el Fragment/Activity.
        // Si tienes una lista de favoritos en el Fragment/Activity,
        // puedes pasarla al adaptador y consultarla aquí.
        if (listener != null) {
            boolean isFav = listener.isLugarFavorite(getSnapshots().getSnapshot(position));
            updateFavoriteIcon(holder.btnFavorite, isFav);
        } else {
            // Estado por defecto si no hay listener o no se puede verificar
            updateFavoriteIcon(holder.btnFavorite, false);
        }

        // --- 2. Configurar Click Listeners ---

        // Clic en el botón de Favorito (corazón)
        holder.btnFavorite.setOnClickListener(v -> {
            if (listener != null && position != RecyclerView.NO_POSITION) {
                DocumentSnapshot snapshot = getSnapshots().getSnapshot(position);
                listener.onFavoriteClick(snapshot, position);
            }
        });

        // Clic en la tarjeta completa (para ir a Detalles)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null && position != RecyclerView.NO_POSITION) {
                DocumentSnapshot snapshot = getSnapshots().getSnapshot(position);
                listener.onLugarClick(snapshot, position);
            }
        });
    }

    @NonNull
    @Override
    public LugarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lugar, parent, false);
        return new LugarViewHolder(view);
    }

    // --- Clase ViewHolder ---
    class LugarViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImagen;
        TextView tvNombre;
        TextView tvDescripcion;
        ImageButton btnFavorite;
        TextView tvRatingValue;
        LinearLayout layoutRating;

        public LugarViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImagen = itemView.findViewById(R.id.item_lugar_imagen);
            tvNombre = itemView.findViewById(R.id.item_lugar_nombre);
            tvDescripcion = itemView.findViewById(R.id.item_lugar_desc);
            btnFavorite = itemView.findViewById(R.id.btn_favorite);
            tvRatingValue = itemView.findViewById(R.id.item_lugar_rating_value);
            layoutRating = itemView.findViewById(R.id.layout_item_rating);
        }
    }

    // --- Interfaz de Clics ---
    public interface OnLugarClickListener {
        void onLugarClick(DocumentSnapshot documentSnapshot, int position);
        void onFavoriteClick(DocumentSnapshot documentSnapshot, int position);
        boolean isLugarFavorite(DocumentSnapshot documentSnapshot); // <-- NUEVO MÉTODO
    }

    public void setOnLugarClickListener(OnLugarClickListener listener) {
        this.listener = listener;
    }

    public static void updateFavoriteIcon(ImageButton button, boolean esFavorito) {
        if (esFavorito) {
            // Color rojo o un icono de corazón lleno
            button.setImageResource(R.drawable.ic_favorite_filled_24dp); // Asume que tienes este drawable
            button.setColorFilter(android.graphics.Color.RED); // Un rojo distintivo
        } else {
            // Color gris o un icono de corazón vacío
            button.setImageResource(R.drawable.ic_favorite_border_24dp); // El que ya tienes
            button.setColorFilter(android.graphics.Color.parseColor("#999999")); // Un gris
        }
    }
}