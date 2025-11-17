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
import androidx.core.app.NavUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FilteredListActivity extends AppCompatActivity implements LugarAdapter.OnLugarClickListener {

    private static final String TAG = "FilteredListActivity";

    // UI Elements
    private TextView tvTitle;
    private ImageButton btnBack;
    private RecyclerView rvFilteredPlaces;
    private TextView tvNoResults; // Para mostrar mensaje si no hay resultados

    // Firebase
    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    // Adapter
    private LugarAdapter adapter;

    // Para manejar los favoritos
    private Set<String> userFavoriteLugarIds = new HashSet<>();

    // El filtro actual
    private String categoryName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filtered_list);

        // --- 1. Inicializar Firebase ---
        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // --- 2. Conectar Vistas ---
        tvTitle = findViewById(R.id.tv_filtered_list_title);
        btnBack = findViewById(R.id.btn_back_filtered_list);
        rvFilteredPlaces = findViewById(R.id.rv_filtered_places);
        tvNoResults = findViewById(R.id.tv_no_results);

        // --- 3. Obtener el parámetro de filtro del Intent ---
        if (getIntent().getExtras() != null) {
            categoryName = getIntent().getStringExtra("CATEGORY_NAME");
            if (categoryName != null && !categoryName.isEmpty()) {
                tvTitle.setText(categoryName); // Establecer el título de la actividad
                Log.d(TAG, "Filtrando por categoría: " + categoryName);
                setupRecyclerView(categoryName); // Configurar el RecyclerView con el filtro
                if (currentUser != null) {
                    loadUserFavorites(currentUser.getUid()); // Cargar favoritos del usuario
                }
            } else {
                Toast.makeText(this, "Error: No se especificó la categoría.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "CATEGORY_NAME es null o vacío.");
                finish();
            }
        } else {
            Toast.makeText(this, "Error: No se recibió información de filtro.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No hay extras en el intent.");
            finish();
        }

        // --- 4. Configurar Click Listeners ---
        btnBack.setOnClickListener(v -> NavUtils.navigateUpFromSameTask(this)); // Navegar hacia arriba
    }

    private void setupRecyclerView(String category) {
        rvFilteredPlaces.setLayoutManager(new LinearLayoutManager(this));

        // Construir la consulta a Firestore, filtrando por el campo 'categoria'
        Query query = mFirestore.collection("lugares")
                .whereEqualTo("categoria", category) // <-- ¡FILTRADO!
                .orderBy("nombre", Query.Direction.ASCENDING); // Ordenar resultados

        FirestoreRecyclerOptions<Lugar> options = new FirestoreRecyclerOptions.Builder<Lugar>()
                .setQuery(query, Lugar.class)
                .build();

        adapter = new LugarAdapter(options);
        rvFilteredPlaces.setAdapter(adapter);
        adapter.setOnLugarClickListener(this);

        // Opcional: Mostrar mensaje si no hay resultados después de un tiempo
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                checkEmptyState();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                checkEmptyState();
            }

            @Override
            public void onChanged() {
                super.onChanged();
                checkEmptyState();
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
        });
    }

    // --- Lógica de Favoritos (similar a LugaresFragment y HomeFragment) ---
    private void loadUserFavorites(String userId) {
        mFirestore.collection("usuarios").document(userId)
                .collection("favoritos")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userFavoriteLugarIds.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        userFavoriteLugarIds.add(doc.getId());
                    }
                    Log.d(TAG, "Favoritos (FilteredList) cargados: " + userFavoriteLugarIds.size() + " lugares.");
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error al cargar favoritos del usuario: " + e.getMessage()));
    }

    @Override
    public void onLugarClick(DocumentSnapshot documentSnapshot, int position) {
        String lugarId = documentSnapshot.getId();
        Intent intent = new Intent(FilteredListActivity.this, LugarDetailActivity.class);
        intent.putExtra("LUGAR_ID", lugarId);
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(DocumentSnapshot documentSnapshot, int position) {
        if (currentUser == null) {
            Toast.makeText(FilteredListActivity.this, "Debes iniciar sesión para agregar a favoritos.", Toast.LENGTH_SHORT).show();
            return;
        }

        String lugarId = documentSnapshot.getId();
        Lugar lugar = documentSnapshot.toObject(Lugar.class);

        if (userFavoriteLugarIds.contains(lugarId)) {
            // Eliminar de favoritos
            mFirestore.collection("usuarios").document(currentUser.getUid())
                    .collection("favoritos").document(lugarId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        userFavoriteLugarIds.remove(lugarId);
                        Toast.makeText(FilteredListActivity.this, "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
                        adapter.notifyItemChanged(position);
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error al eliminar favorito: " + e.getMessage()));
        } else {
            // Agregar a favoritos
            if (lugar != null) {
                Map<String, Object> favoritoData = new HashMap<>();
                favoritoData.put("lugarId", lugarId);
                favoritoData.put("nombre", lugar.getNombre());
                favoritoData.put("urlImagen", lugar.getUrlImagen());
                favoritoData.put("direccion", lugar.getDireccion());
                favoritoData.put("ratingPromedio", lugar.getRatingPromedio());
                favoritoData.put("fechaAgregado", FieldValue.serverTimestamp());

                mFirestore.collection("usuarios").document(currentUser.getUid())
                        .collection("favoritos").document(lugarId)
                        .set(favoritoData)
                        .addOnSuccessListener(aVoid -> {
                            userFavoriteLugarIds.add(lugarId);
                            Toast.makeText(FilteredListActivity.this, "Agregado a favoritos", Toast.LENGTH_SHORT).show();
                            adapter.notifyItemChanged(position);
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "Error al agregar favorito: " + e.getMessage()));
            }
        }
    }

    @Override
    public boolean isLugarFavorite(DocumentSnapshot documentSnapshot) {
        if (currentUser == null) {
            return false;
        }
        return userFavoriteLugarIds.contains(documentSnapshot.getId());
    }

    // --- Manejo del Ciclo de Vida del Adaptador ---
    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
        // Recargar favoritos por si el estado cambió en otra pantalla
        if (currentUser != null && categoryName != null) {
            loadUserFavorites(currentUser.getUid());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }
}
