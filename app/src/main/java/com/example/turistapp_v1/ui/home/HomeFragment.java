package com.example.turistapp_v1.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Imports importantes
import com.example.turistapp_v1.Category;
import com.example.turistapp_v1.CategoryAdapter;
import com.example.turistapp_v1.FilteredListActivity;
import com.example.turistapp_v1.Lugar;
import com.example.turistapp_v1.LugarAdapter;
import com.example.turistapp_v1.LugarDetailActivity;
import com.example.turistapp_v1.R;
import com.example.turistapp_v1.databinding.FragmentHomeBinding;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue; // Importar
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap; // Importar
import java.util.HashSet; // Importar
import java.util.List;
import java.util.Map;   // Importar
import java.util.Set;   // Importar

// El Fragmento implementa la interfaz de clics de ambos adaptadores
public class HomeFragment extends Fragment implements CategoryAdapter.OnCategoryClickListener, LugarAdapter.OnLugarClickListener {

    private static final String TAG = "HomeFragment";

    private FragmentHomeBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private FirebaseUser currentUser; // Variable para el usuario

    // Categorías Populares
    private RecyclerView rvPopularCategories;
    private CategoryAdapter categoryAdapter;

    // Lugares Recomendados
    private RecyclerView rvRecommendedPlaces;
    private LugarAdapter lugarAdapter; // ¡Nuestro adaptador de lugares!

    // --- NUEVO: Conjunto para guardar los IDs de favoritos ---
    private Set<String> userFavoriteLugarIds = new HashSet<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Conectar vistas
        rvPopularCategories = binding.rvPopularCategories;
        rvRecommendedPlaces = binding.rvRecommendedPlaces;

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser(); // Obtener usuario

        // Configurar la bienvenida al usuario
        setupUserGreeting();

        // Configurar las categorías populares
        setupPopularCategories();

        // Configurar los lugares recomendados
        setupRecommendedPlaces();

        // --- NUEVO: Cargar favoritos del usuario (si está logueado) ---
        if (currentUser != null) {
            loadUserFavorites(currentUser.getUid());
        }
    }

    private void setupUserGreeting() {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            mFirestore.collection("usuarios").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String userName = documentSnapshot.getString("nombre");
                            if (userName != null) {
                                binding.tvGreetingName.setText(userName);
                            } else {
                                binding.tvGreetingName.setText("Viajero");
                            }
                        } else {
                            binding.tvGreetingName.setText("Viajero");
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error al cargar nombre de usuario", e));

            // Configurar saludo de "Buenos días/tardes/noches"
            TextView tvGreetingHi = binding.tvGreetingHi;
            Calendar calendar = Calendar.getInstance();
            int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);

            if (hourOfDay >= 0 && hourOfDay < 12) {
                tvGreetingHi.setText("Buenos días,");
            } else if (hourOfDay >= 12 && hourOfDay < 18) {
                tvGreetingHi.setText("Buenas tardes,");
            } else {
                tvGreetingHi.setText("Buenas noches,");
            }

        } else {
            binding.tvGreetingName.setText("Invitado");
            binding.tvGreetingHi.setText("Bienvenido,");
        }
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

    // --- NUEVO MÉTODO: Cargar los favoritos del usuario (igual que en LugaresFragment) ---
    private void loadUserFavorites(String userId) {
        mFirestore.collection("usuarios").document(userId)
                .collection("favoritos")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userFavoriteLugarIds.clear(); // Limpiar antes de recargar
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        userFavoriteLugarIds.add(doc.getId());
                    }
                    Log.d(TAG, "Favoritos (Home) cargados: " + userFavoriteLugarIds.size() + " lugares.");
                    if (lugarAdapter != null) {
                        lugarAdapter.notifyDataSetChanged(); // Notificar al adaptador
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar favoritos del usuario: " + e.getMessage());
                });
    }


    private void setupRecommendedPlaces() {
        rvRecommendedPlaces.setLayoutManager(new LinearLayoutManager(getContext()));

        Query query = mFirestore.collection("lugares")
                .orderBy("fechaCreacion", Query.Direction.DESCENDING)
                .limit(5);

        FirestoreRecyclerOptions<Lugar> options = new FirestoreRecyclerOptions.Builder<Lugar>()
                .setQuery(query, Lugar.class)
                .build();

        lugarAdapter = new LugarAdapter(options);
        rvRecommendedPlaces.setAdapter(lugarAdapter);
        lugarAdapter.setOnLugarClickListener(this);
    }

    // --- IMPLEMENTACIÓN DE LOS CLICS DE CategoryAdapter ---

    @Override
    public void onCategoryClick(String categoryName) {
        Toast.makeText(getContext(), "Clic en categoría: " + categoryName, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Categoría clicada: " + categoryName);

        // --- ¡AQUÍ ESTÁ EL CAMBIO! ---
        Intent intent = new Intent(getActivity(), FilteredListActivity.class);
        intent.putExtra("CATEGORY_NAME", categoryName); // Pasa el nombre de la categoría
        startActivity(intent);
    }

    // --- IMPLEMENTACIÓN DE LOS CLICS DE LugarAdapter ---
    @Override
    public void onLugarClick(DocumentSnapshot documentSnapshot, int position) {
        String lugarId = documentSnapshot.getId();
        Log.d(TAG, "Clic en lugar recomendado. ID: " + lugarId);

        Intent intent = new Intent(getActivity(), LugarDetailActivity.class);
        intent.putExtra("LUGAR_ID", lugarId);
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(DocumentSnapshot documentSnapshot, int position) {
        // --- LÓGICA DE FAVORITOS (copiada de LugaresFragment) ---
        if (currentUser == null) {
            Toast.makeText(getContext(), "Debes iniciar sesión para agregar a favoritos.", Toast.LENGTH_SHORT).show();
            return;
        }

        String lugarId = documentSnapshot.getId();
        Lugar lugar = documentSnapshot.toObject(Lugar.class);

        if (userFavoriteLugarIds.contains(lugarId)) {
            // Ya es favorito, eliminar
            mFirestore.collection("usuarios").document(currentUser.getUid())
                    .collection("favoritos").document(lugarId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        userFavoriteLugarIds.remove(lugarId);
                        Toast.makeText(getContext(), "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
                        lugarAdapter.notifyItemChanged(position); // Actualizar solo este item
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error al eliminar favorito: " + e.getMessage()));
        } else {
            // No es favorito, agregar
            if (lugar != null) {
                Map<String, Object> favoritoData = new HashMap<>();
                favoritoData.put("lugarId", lugarId);
                favoritoData.put("nombre", lugar.getNombre());
                favoritoData.put("urlImagen", lugar.getUrlImagen());
                favoritoData.put("direccion", lugar.getDireccion()); // Guardar dirección
                favoritoData.put("ratingPromedio", lugar.getRatingPromedio()); // Guardar rating
                favoritoData.put("fechaAgregado", FieldValue.serverTimestamp());

                mFirestore.collection("usuarios").document(currentUser.getUid())
                        .collection("favoritos").document(lugarId)
                        .set(favoritoData)
                        .addOnSuccessListener(aVoid -> {
                            userFavoriteLugarIds.add(lugarId);
                            Toast.makeText(getContext(), "Agregado a favoritos", Toast.LENGTH_SHORT).show();
                            lugarAdapter.notifyItemChanged(position); // Actualizar solo este item
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


    // --- Manejar el ciclo de vida del Adaptador de Lugares ---
    @Override
    public void onStart() {
        super.onStart();
        if (lugarAdapter != null) {
            lugarAdapter.startListening();
        }
        // Recargar favoritos por si el estado cambió en otra pantalla
        if (currentUser != null) {
            loadUserFavorites(currentUser.getUid());
        }
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