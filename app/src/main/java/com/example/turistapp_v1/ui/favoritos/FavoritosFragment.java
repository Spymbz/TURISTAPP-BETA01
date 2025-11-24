package com.example.turistapp_v1.ui.favoritos;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.turistapp_v1.FavoritesDBHelper;
import com.example.turistapp_v1.FavoritosAdapter;
import com.example.turistapp_v1.Lugar;
import com.example.turistapp_v1.LugarDetailActivity;
import com.example.turistapp_v1.SearchActivity;
import com.example.turistapp_v1.databinding.FragmentFavoritosBinding;

import java.util.ArrayList;
import java.util.List;

public class FavoritosFragment extends Fragment implements FavoritosAdapter.OnLugarClickListener {

    private FragmentFavoritosBinding binding;
    private FavoritesDBHelper dbHelper;
    private FavoritosAdapter adapter;
    private List<Lugar> favoriteLugares = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFavoritosBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new FavoritesDBHelper(getContext());

        // Listener para la barra de búsqueda falsa
        binding.searchLayoutFavoritos.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SearchActivity.class);
            intent.putExtra("IS_FAVORITES_SEARCH", true); // ¡Esta es la línea clave!
            startActivity(intent);
            getActivity().overridePendingTransition(0, 0);
        });

        setupRecyclerView();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavoritesFromDB();
    }

    private void setupRecyclerView() {
        binding.rvFavoritos.setLayoutManager(new LinearLayoutManager(getContext()));
        // Se crea el adaptador con la lista (inicialmente vacía) y el listener.
        adapter = new FavoritosAdapter(favoriteLugares, this);
        binding.rvFavoritos.setAdapter(adapter);
    }

    private void loadFavoritesFromDB() {
        // Obtenemos la lista de lugares directamente desde el DBHelper.
        favoriteLugares = dbHelper.getAllFavoriteLugares();

        if (favoriteLugares.isEmpty()) {
            // Si no hay favoritos, mostramos el mensaje y ocultamos la lista.
            binding.layoutLoginPrompt.setVisibility(View.VISIBLE);
            binding.rvFavoritos.setVisibility(View.GONE);
        } else {
            // Si hay favoritos, ocultamos el mensaje y mostramos la lista.
            binding.layoutLoginPrompt.setVisibility(View.GONE);
            binding.rvFavoritos.setVisibility(View.VISIBLE);
        }

        // Actualizamos los datos en el adaptador para que la lista se repinte.
        adapter.setLugares(favoriteLugares);
    }

    @Override
    public void onLugarClick(String lugarId, Lugar lugar) {
        Intent intent = new Intent(getActivity(), LugarDetailActivity.class);
        intent.putExtra("LUGAR_ID", lugarId);
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(String lugarId, Lugar lugar, int position) {
        // Eliminamos el favorito de la base de datos local.
        dbHelper.removeFavorite(lugarId);
        // Eliminamos el lugar de la lista en el adaptador para una actualización visual instantánea.
        adapter.removerLugar(position);
        Toast.makeText(getContext(), "Eliminado de favoritos", Toast.LENGTH_SHORT).show();

        // Si la lista queda vacía después de eliminar, mostramos el mensaje.
        if (adapter.getItemCount() == 0) {
            binding.layoutLoginPrompt.setVisibility(View.VISIBLE);
            binding.rvFavoritos.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
