package com.example.turistapp_v1.ui.home;

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
import androidx.recyclerview.widget.RecyclerView;

import com.example.turistapp_v1.Category;
import com.example.turistapp_v1.CategoryAdapter;
import com.example.turistapp_v1.FavoritesDBHelper;
import com.example.turistapp_v1.FilteredListActivity;
import com.example.turistapp_v1.Lugar;
import com.example.turistapp_v1.LugarAdapter;
import com.example.turistapp_v1.LugarDetailActivity;
import com.example.turistapp_v1.R;
import com.example.turistapp_v1.SearchActivity;
import com.example.turistapp_v1.databinding.FragmentHomeBinding;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HomeFragment extends Fragment implements CategoryAdapter.OnCategoryClickListener, LugarAdapter.OnLugarClickListener {

    private FragmentHomeBinding binding;
    private DatabaseReference mDatabase;
    private FavoritesDBHelper dbHelper;

    private RecyclerView rvPopularCategories;
    private CategoryAdapter categoryAdapter;

    private RecyclerView rvRecommendedPlaces;
    private LugarAdapter lugarAdapter;

    private Set<String> userFavoriteLugarIds = new HashSet<>();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        dbHelper = new FavoritesDBHelper(getContext());

        rvPopularCategories = binding.rvPopularCategories;
        rvRecommendedPlaces = binding.rvRecommendedPlaces;

        setupPopularCategories();
        setupRecommendedPlaces();
        loadUserFavorites();

        View fakeSearchBar = binding.fakeSearchBar;

        fakeSearchBar.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SearchActivity.class);
            startActivity(intent);
            getActivity().overridePendingTransition(0, 0);
        });
    }

    private void setupPopularCategories() {
        List<Category> categories = new ArrayList<>();
        categories.add(new Category("Playas", R.drawable.playa));
        categories.add(new Category("Parques", R.drawable.parque));
        categories.add(new Category("Monumentos", R.drawable.monumento));
        categories.add(new Category("Museos", R.drawable.museo));
        categories.add(new Category("Iglesias", R.drawable.iglesia));
        categories.add(new Category("Observatorios", R.drawable.observatorio));

        categoryAdapter = new CategoryAdapter(categories, this);
        rvPopularCategories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvPopularCategories.setAdapter(categoryAdapter);
    }

    private void loadUserFavorites() {
        // Obtenemos la lista de lugares favoritos completa
        List<Lugar> favoriteLugares = dbHelper.getAllFavoriteLugares();
        // Extraemos solo los IDs y los guardamos en nuestro Set
        userFavoriteLugarIds = favoriteLugares.stream().map(Lugar::getId).collect(Collectors.toSet());

        if (lugarAdapter != null) {
            lugarAdapter.notifyDataSetChanged();
        }
    }

    private void setupRecommendedPlaces() {
        rvRecommendedPlaces.setLayoutManager(new LinearLayoutManager(getContext()));

        // La consulta para traer los últimos 2 lugares.
        Query query = mDatabase.child("lugares").orderByChild("fechaCreacion").limitToLast(3);

        FirebaseRecyclerOptions<Lugar> options = new FirebaseRecyclerOptions.Builder<Lugar>()
                .setQuery(query, Lugar.class)
                .build();

        lugarAdapter = new LugarAdapter(options);
        rvRecommendedPlaces.setAdapter(lugarAdapter);
        lugarAdapter.setOnLugarClickListener(this);

        lugarAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                binding.rvRecommendedPlaces.post(binding.rvRecommendedPlaces::requestLayout);
            }

            @Override
            public void onChanged() {
                super.onChanged();
                binding.rvRecommendedPlaces.post(binding.rvRecommendedPlaces::requestLayout);
            }
        });
    }

    @Override
    public void onCategoryClick(String categoryName) {
        Intent intent = new Intent(getActivity(), FilteredListActivity.class);
        intent.putExtra("CATEGORY_NAME", categoryName);
        startActivity(intent);
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
            // Pasamos el objeto Lugar completo, no solo el ID
            dbHelper.addFavorite(lugar);
            userFavoriteLugarIds.add(lugarId);
            Toast.makeText(getContext(), "Agregado a favoritos", Toast.LENGTH_SHORT).show();
        }
        lugarAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean isLugarFavorite(String lugarId) {
        return userFavoriteLugarIds.contains(lugarId);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (lugarAdapter != null) {
            lugarAdapter.startListening();
        }
        loadUserFavorites();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (lugarAdapter != null) {
            lugarAdapter.stopListening();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
