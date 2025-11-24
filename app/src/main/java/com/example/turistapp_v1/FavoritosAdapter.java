package com.example.turistapp_v1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

// Adaptador para una lista local de lugares, no depende de Firebase.
public class FavoritosAdapter extends RecyclerView.Adapter<FavoritosAdapter.LugarViewHolder> {

    private List<Lugar> lugares;
    private OnLugarClickListener listener;

    public FavoritosAdapter(List<Lugar> lugares, OnLugarClickListener listener) {
        this.lugares = lugares;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LugarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lugar, parent, false);
        return new LugarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LugarViewHolder holder, int position) {
        Lugar lugar = lugares.get(position);

        holder.tvNombre.setText(lugar.getNombre());
        holder.tvDescripcion.setText(lugar.getDireccion());

        if (lugar.getUrlImagen() != null && !lugar.getUrlImagen().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(lugar.getUrlImagen())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(holder.ivImagen);
        } else {
            holder.ivImagen.setImageResource(R.drawable.placeholder_image);
        }

        String lugarId = lugar.getId();

        updateFavoriteIcon(holder.btnFavorite, true);

        holder.btnFavorite.setOnClickListener(v -> {
            if (listener != null && lugarId != null) {
                listener.onFavoriteClick(lugarId, lugar, holder.getAdapterPosition());
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null && lugarId != null) {
                listener.onLugarClick(lugarId, lugar);
            }
        });
    }

    @Override
    public int getItemCount() {
        return lugares == null ? 0 : lugares.size();
    }

    public void setLugares(List<Lugar> nuevosLugares) {
        this.lugares = nuevosLugares;
        notifyDataSetChanged();
    }
    
    public void removerLugar(int position) {
        if (lugares != null && position >= 0 && position < lugares.size()) {
            lugares.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, lugares.size());
        }
    }

    class LugarViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImagen;
        TextView tvNombre;
        TextView tvDescripcion;
        ImageButton btnFavorite;

        public LugarViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImagen = itemView.findViewById(R.id.item_lugar_imagen);
            tvNombre = itemView.findViewById(R.id.item_lugar_nombre);
            tvDescripcion = itemView.findViewById(R.id.item_lugar_desc);
            btnFavorite = itemView.findViewById(R.id.btn_favorite);
        }
    }

    // Interfaz de listener modificada para incluir la posición en el clic de favorito
    public interface OnLugarClickListener {
        void onLugarClick(String lugarId, Lugar lugar);
        void onFavoriteClick(String lugarId, Lugar lugar, int position);
    }

    public static void updateFavoriteIcon(ImageButton button, boolean esFavorito) {
        if (esFavorito) {
            button.setImageResource(R.drawable.ic_favorite_filled_24dp);
            button.setColorFilter(android.graphics.Color.RED);
        } else {
            button.setImageResource(R.drawable.ic_favorite_border_24dp);
            button.setColorFilter(android.graphics.Color.parseColor("#999999"));
        }
    }
}
