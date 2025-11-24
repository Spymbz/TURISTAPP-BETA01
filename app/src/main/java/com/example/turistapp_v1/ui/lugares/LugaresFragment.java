package com.example.turistapp_v1.ui.lugares;

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
import com.example.turistapp_v1.Lugar;
import com.example.turistapp_v1.LugarAdapter;
import com.example.turistapp_v1.LugarDetailActivity;
import com.example.turistapp_v1.SearchActivity;
import com.example.turistapp_v1.databinding.FragmentLugaresBinding;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LugaresFragment extends Fragment implements LugarAdapter.OnLugarClickListener {

    private FragmentLugaresBinding binding;
    private DatabaseReference mDatabase;
    private FavoritesDBHelper dbHelper;
    private LugarAdapter adapter;
    private Set<String> userFavoriteLugarIds = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLugaresBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        dbHelper = new FavoritesDBHelper(getContext());

        setupRecyclerView();
        setupSearch();
    }

    private void setupRecyclerView() {
        binding.rvLugares.setLayoutManager(new LinearLayoutManager(getContext()));
        Query query = mDatabase.child("lugares").orderByChild("nombre");
        FirebaseRecyclerOptions<Lugar> options = new FirebaseRecyclerOptions.Builder<Lugar>()
                .setQuery(query, Lugar.class)
                .build();
        adapter = new LugarAdapter(options);
        adapter.setOnLugarClickListener(this);
        binding.rvLugares.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.fakeSearchBarLugares.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SearchActivity.class);
            startActivity(intent);
            getActivity().overridePendingTransition(0, 0);
        });
    }

    private void loadUserFavorites() {
        userFavoriteLugarIds.clear();
        List<Lugar> favoriteLugares = dbHelper.getAllFavoriteLugares();
        for (Lugar lugar : favoriteLugares) {
            if (lugar != null && lugar.getId() != null) {
                userFavoriteLugarIds.add(lugar.getId());
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onLugarClick(String lugarId, Lugar lugar) {
        Intent intent = new Intent(getActivity(), LugarDetailActivity.class);
        intent.putExtra("LUGAR_ID", lugarId);
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(String lugarId, Lugar lugar) {
        if (userFavoriteLugarIds.contains(lugarId)) {
            dbHelper.removeFavorite(lugarId);
            userFavoriteLugarIds.remove(lugarId);
            Toast.makeText(getContext(), "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
        } else {
            if (lugar != null) {
                lugar.setId(lugarId);
                dbHelper.addFavorite(lugar);
                userFavoriteLugarIds.add(lugarId);
                Toast.makeText(getContext(), "Agregado a favoritos", Toast.LENGTH_SHORT).show();
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean isLugarFavorite(String lugarId) {
        return userFavoriteLugarIds.contains(lugarId);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
        loadUserFavorites();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
