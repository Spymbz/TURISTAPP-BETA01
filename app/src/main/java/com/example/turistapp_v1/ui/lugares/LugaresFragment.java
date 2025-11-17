package com.example.turistapp_v1.ui.lugares;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.turistapp_v1.AddLugarActivity;
import com.example.turistapp_v1.Lugar;
import com.example.turistapp_v1.LugarAdapter;
import com.example.turistapp_v1.LugarDetailActivity;
import com.example.turistapp_v1.R;
import com.example.turistapp_v1.databinding.FragmentLugaresBinding;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

public class LugaresFragment extends Fragment implements LugarAdapter.OnLugarClickListener {

    private static final String TAG = "LugaresFragment";

    private FragmentLugaresBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;

    private RecyclerView rvLugares;

    private LugarAdapter adapter;
    private FirebaseUser currentUser; // Variable para el usuario actual

    // --- NUEVO: Conjunto para guardar los IDs de lugares favoritos del usuario actual ---
    private Set<String> userFavoriteLugarIds = new HashSet<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLugaresBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        rvLugares = binding.rvLugares;


        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser(); // Obtener el usuario actual aquí

        if (currentUser != null) {
            loadUserFavorites(currentUser.getUid()); // <-- NUEVO: Cargar favoritos al iniciar
        }

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        rvLugares.setLayoutManager(new LinearLayoutManager(getContext()));

        Query query = mFirestore.collection("lugares")
                .orderBy("nombre", Query.Direction.ASCENDING);

        FirestoreRecyclerOptions<Lugar> options = new FirestoreRecyclerOptions.Builder<Lugar>()
                .setQuery(query, Lugar.class)
                .build();

        adapter = new LugarAdapter(options);
        rvLugares.setAdapter(adapter);
        adapter.setOnLugarClickListener(this);
    }

    // --- NUEVO MÉTODO: Cargar los favoritos del usuario desde Firestore ---
    private void loadUserFavorites(String userId) {
        mFirestore.collection("usuarios").document(userId)
                .collection("favoritos")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userFavoriteLugarIds.clear(); // Limpiar antes de recargar
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        userFavoriteLugarIds.add(doc.getId());
                    }
                    Log.d(TAG, "Favoritos cargados: " + userFavoriteLugarIds.size() + " lugares.");
                    adapter.notifyDataSetChanged(); // Notificar al adaptador que los datos pueden haber cambiado
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar favoritos del usuario: " + e.getMessage());
                });
    }


    // --- IMPLEMENTACIÓN DE LOS CLICS DEL ADAPTADOR ---

    @Override
    public void onLugarClick(DocumentSnapshot documentSnapshot, int position) {
        String lugarId = documentSnapshot.getId();
        Intent intent = new Intent(getActivity(), LugarDetailActivity.class);
        intent.putExtra("LUGAR_ID", lugarId);
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(DocumentSnapshot documentSnapshot, int position) {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Debes iniciar sesión para agregar a favoritos.", Toast.LENGTH_SHORT).show();
            return;
        }

        String lugarId = documentSnapshot.getId();
        Lugar lugar = documentSnapshot.toObject(Lugar.class); // Obtener el objeto Lugar completo

        if (userFavoriteLugarIds.contains(lugarId)) {
            // Ya es favorito, eliminar
            mFirestore.collection("usuarios").document(currentUser.getUid())
                    .collection("favoritos").document(lugarId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        userFavoriteLugarIds.remove(lugarId);
                        Toast.makeText(getContext(), "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
                        adapter.notifyItemChanged(position); // Actualizar solo este item
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error al eliminar de favoritos", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error al eliminar favorito: " + e.getMessage());
                    });
        } else {
            // No es favorito, agregar
            if (lugar != null) {
                Map<String, Object> favoritoData = new HashMap<>();
                favoritoData.put("lugarId", lugarId);
                favoritoData.put("nombre", lugar.getNombre());
                favoritoData.put("urlImagen", lugar.getUrlImagen());
                favoritoData.put("direccion", lugar.getDireccion()); // Guardar dirección
                favoritoData.put("ratingPromedio", lugar.getRatingPromedio()); // <-- ¡LA LÍNEA QUE FALTABA!
                favoritoData.put("fechaAgregado", FieldValue.serverTimestamp());

                mFirestore.collection("usuarios").document(currentUser.getUid())
                        .collection("favoritos").document(lugarId)
                        .set(favoritoData)
                        .addOnSuccessListener(aVoid -> {
                            userFavoriteLugarIds.add(lugarId);
                            Toast.makeText(getContext(), "Agregado a favoritos", Toast.LENGTH_SHORT).show();
                            adapter.notifyItemChanged(position); // Actualizar solo este item
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Error al agregar a favoritos", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error al agregar favorito: " + e.getMessage());
                        });
            }
        }
    }

    // --- NUEVO MÉTODO: Implementar la verificación de favorito ---
    @Override
    public boolean isLugarFavorite(DocumentSnapshot documentSnapshot) {
        if (currentUser == null) {
            return false; // Si no hay usuario, no hay favoritos
        }
        return userFavoriteLugarIds.contains(documentSnapshot.getId());
    }


    // --- Manejar el ciclo de vida del Adaptador ---
    @Override
    public void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
        // Recargar favoritos por si el estado cambió en LugarDetailActivity
        if (currentUser != null) {
            loadUserFavorites(currentUser.getUid());
        }
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