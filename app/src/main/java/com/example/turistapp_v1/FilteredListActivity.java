package com.example.turistapp_v1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FilteredListActivity extends AppCompatActivity implements LugarAdapter.OnLugarClickListener {

    private TextView tvTitle;
    private ImageButton btnBack;
    private RecyclerView rvFilteredPlaces;
    private TextView tvNoResults;

    private DatabaseReference mDatabase;
    private FavoritesDBHelper dbHelper;
    private LugarAdapter adapter;

    private Set<String> userFavoriteLugarIds = new HashSet<>();
    private String categoryName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filtered_list);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        dbHelper = new FavoritesDBHelper(this);

        tvTitle = findViewById(R.id.tv_filtered_list_title);
        btnBack = findViewById(R.id.btn_back_filtered_list);
        rvFilteredPlaces = findViewById(R.id.rv_filtered_places);
        tvNoResults = findViewById(R.id.tv_no_results);

        categoryName = getIntent().getStringExtra("CATEGORY_NAME");

        if (categoryName != null && !categoryName.isEmpty()) {
            tvTitle.setText(categoryName);
            setupRecyclerView(categoryName);
        } else {
            Toast.makeText(this, "Error: Categoría no especificada.", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView(String category) {
        rvFilteredPlaces.setLayoutManager(new LinearLayoutManager(this));

        Query query = mDatabase.child("lugares").orderByChild("categoria").equalTo(category);

        FirebaseRecyclerOptions<Lugar> options = new FirebaseRecyclerOptions.Builder<Lugar>()
                .setQuery(query, Lugar.class)
                .build();

        adapter = new LugarAdapter(options);
        rvFilteredPlaces.setAdapter(adapter);
        adapter.setOnLugarClickListener(this);

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                checkEmptyState();
            }

            @Override
            public void onChanged() {
                super.onChanged();
                checkEmptyState();
            }
        });
    }

    private void checkEmptyState() {
        if (adapter.getItemCount() == 0) {
            tvNoResults.setVisibility(View.VISIBLE);
            rvFilteredPlaces.setVisibility(View.GONE);
        } else {
            tvNoResults.setVisibility(View.GONE);
            rvFilteredPlaces.setVisibility(View.VISIBLE);
        }
    }

    private void loadUserFavorites() {
        List<Lugar> favoriteLugares = dbHelper.getAllFavoriteLugares();
        userFavoriteLugarIds.clear();
        for (Lugar lugar : favoriteLugares) {
            userFavoriteLugarIds.add(lugar.getId());
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
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
        if (userFavoriteLugarIds.contains(lugarId)) {
            dbHelper.removeFavorite(lugarId);
            userFavoriteLugarIds.remove(lugarId);
            Toast.makeText(this, "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
        } else {
            dbHelper.addFavorite(lugar);
            userFavoriteLugarIds.add(lugarId);
            Toast.makeText(this, "Agregado a favoritos", Toast.LENGTH_SHORT).show();
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean isLugarFavorite(String lugarId) {
        return userFavoriteLugarIds.contains(lugarId);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
        loadUserFavorites();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }
}
