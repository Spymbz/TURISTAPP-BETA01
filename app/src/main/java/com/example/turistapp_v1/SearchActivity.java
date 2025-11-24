package com.example.turistapp_v1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// Implementamos los listeners de AMBOS adaptadores
public class SearchActivity extends AppCompatActivity implements LugarAdapter.OnLugarClickListener, FavoritosAdapter.OnLugarClickListener {

    private SearchView searchView;
    private RecyclerView rvResults;
    private TextView tvEmptyText;

    // Dependencias
    private DatabaseReference mDatabase;
    private FavoritesDBHelper dbHelper;

    // Adaptadores y datos
    private LugarAdapter lugarAdapter; // Para búsqueda general en Firebase
    private FavoritosAdapter favoritosAdapter; // Para búsqueda local en favoritos
    private List<Lugar> allFavorites; // Lista que mantiene todos los favoritos

    private boolean isFavoritesSearch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Inicialización de vistas y dependencias
        mDatabase = FirebaseDatabase.getInstance().getReference();
        dbHelper = new FavoritesDBHelper(this);
        searchView = findViewById(R.id.search_view_real);
        rvResults = findViewById(R.id.rv_search_results);
        tvEmptyText = findViewById(R.id.tv_search_empty);
        rvResults.setLayoutManager(new LinearLayoutManager(this));

        // Determinamos el modo de búsqueda
        isFavoritesSearch = getIntent().getBooleanExtra("IS_FAVORITES_SEARCH", false);

        if (isFavoritesSearch) {
            setupFavoritesSearch();
        } else {
            setupGeneralSearch();
        }

        searchView.requestFocus();
    }

    private void setupGeneralSearch() {
        tvEmptyText.setText("Escribe para buscar...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                firebaseSearch(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                firebaseSearch(newText);
                return false;
            }
        });
    }

    private void setupFavoritesSearch() {
        allFavorites = dbHelper.getAllFavoriteLugares();
        favoritosAdapter = new FavoritosAdapter(new ArrayList<>(allFavorites), this);
        rvResults.setAdapter(favoritosAdapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterFavorites(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterFavorites(newText);
                return false;
            }
        });

        checkFavoritesEmptyState();
    }

    private void filterFavorites(String searchText) {
        String queryText = searchText.toLowerCase().trim();

        if (queryText.isEmpty()) {
            favoritosAdapter.setLugares(new ArrayList<>(allFavorites));
            checkFavoritesEmptyState();
            return;
        }

        List<Lugar> filteredList = allFavorites.stream()
                .filter(lugar -> lugar.getNombre().toLowerCase().contains(queryText))
                .collect(Collectors.toList());

        favoritosAdapter.setLugares(filteredList);
        checkFavoritesEmptyState();
    }

    private void firebaseSearch(String searchText) {
        String queryText = searchText.toLowerCase().trim();

        if (queryText.isEmpty()) {
            if (lugarAdapter != null) {
                lugarAdapter.stopListening();
            }
            rvResults.setAdapter(null);
            tvEmptyText.setVisibility(View.VISIBLE);
            tvEmptyText.setText("Escribe para buscar...");
            return;
        }

        Query query = mDatabase.child("lugares")
                .orderByChild("nombreMinuscula")
                .startAt(queryText)
                .endAt(queryText + "");

        FirebaseRecyclerOptions<Lugar> options = new FirebaseRecyclerOptions.Builder<Lugar>()
                .setQuery(query, Lugar.class)
                .build();

        if (lugarAdapter != null) {
            lugarAdapter.stopListening();
        }

        lugarAdapter = new LugarAdapter(options);
        rvResults.setAdapter(lugarAdapter);
        lugarAdapter.setOnLugarClickListener(this);
        lugarAdapter.startListening();

        lugarAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                checkGeneralEmptyState();
            }

            @Override
            public void onChanged() {
                super.onChanged();
                checkGeneralEmptyState();
            }
        });
        checkGeneralEmptyState();
    }

    private void checkGeneralEmptyState() {
        boolean isEmpty = lugarAdapter == null || lugarAdapter.getItemCount() == 0;
        tvEmptyText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvResults.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        if (isEmpty) {
            tvEmptyText.setText("No se encontraron resultados.");
        }
    }

    private void checkFavoritesEmptyState() {
        boolean isEmpty = favoritosAdapter == null || favoritosAdapter.getItemCount() == 0;
        tvEmptyText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvResults.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        if (isEmpty) {
            if (allFavorites.isEmpty()) {
                tvEmptyText.setText("No tienes lugares favoritos.");
            } else {
                tvEmptyText.setText("No se encontraron favoritos con ese nombre.");
            }
        }
    }

    @Override
    public void onLugarClick(String lugarId, Lugar lugar) {
        Intent intent = new Intent(this, LugarDetailActivity.class);
        intent.putExtra("LUGAR_ID", lugarId);
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(String lugarId, Lugar lugar) {
        if (dbHelper.isFavorite(lugarId)) {
            dbHelper.removeFavorite(lugarId);
        } else {
            dbHelper.addFavorite(lugar);
        }
        if (lugarAdapter != null) {
            lugarAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onFavoriteClick(String lugarId, Lugar lugar, int position) {
        dbHelper.removeFavorite(lugarId);
        allFavorites.removeIf(l -> l.getId().equals(lugarId));
        favoritosAdapter.removerLugar(position);
        checkFavoritesEmptyState();
    }


    @Override
    public boolean isLugarFavorite(String lugarId) {
        return dbHelper.isFavorite(lugarId);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (lugarAdapter != null) {
            lugarAdapter.stopListening();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (lugarAdapter != null && !isFavoritesSearch) {
            lugarAdapter.startListening();
        }
    }
}
