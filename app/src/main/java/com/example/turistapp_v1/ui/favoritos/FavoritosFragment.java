package com.example.turistapp_v1.ui.favoritos;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout; // Importar LinearLayout
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Imports necesarios
import com.example.turistapp_v1.LoginActivity;
import com.example.turistapp_v1.Lugar;
import com.example.turistapp_v1.LugarAdapter; // ¡Reutilizamos el adaptador!
import com.example.turistapp_v1.LugarDetailActivity;
import com.example.turistapp_v1.R;
import com.example.turistapp_v1.databinding.FragmentFavoritosBinding;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashSet;
import java.util.Set;

// Hacemos que el Fragmento implemente la interfaz de clics
public class FavoritosFragment extends Fragment implements LugarAdapter.OnLugarClickListener {

    private static final String TAG = "FavoritosFragment";

    // --- Variables de UI y Firebase ---
    private FragmentFavoritosBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private FirebaseUser currentUser;

    // Elementos del Layout
    private RecyclerView rvFavoritos;
    private LinearLayout layoutLoginPrompt; // El layout para invitados
    private Button btnGoToLogin;

    // Adaptador
    private LugarAdapter adapter;

    // Lista para saber qué es favorito (aunque aquí, todo lo es)
    private Set<String> userFavoriteLugarIds = new HashSet<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentFavoritosBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Conectar Vistas
        rvFavoritos = binding.rvFavoritos;
        layoutLoginPrompt = binding.layoutLoginPrompt;
        btnGoToLogin = binding.btnGoToLogin;

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Configurar el RecyclerView (layout manager)
        rvFavoritos.setLayoutManager(new LinearLayoutManager(getContext()));

        // Configurar el botón de "Iniciar Sesión" (para invitados)
        btnGoToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
        });
    }

    // --- Lógica principal ---
    private void checkUserStatus() {
        if (currentUser == null) {
            // --- Usuario NO Logueado (Invitado) ---
            layoutLoginPrompt.setVisibility(View.VISIBLE); // Mostrar prompt
            rvFavoritos.setVisibility(View.GONE); // Ocultar lista

            // Detener el adaptador si estaba escuchando
            if (adapter != null) {
                adapter.stopListening();
            }
        } else {
            // --- Usuario SÍ Logueado ---
            layoutLoginPrompt.setVisibility(View.GONE); // Ocultar prompt
            rvFavoritos.setVisibility(View.VISIBLE); // Mostrar lista

            // Cargar la lista de favoritos del usuario
            loadUserFavorites(currentUser.getUid());
        }
    }

    private void loadUserFavorites(String userId) {
        Log.d(TAG, "Cargando favoritos para el usuario: " + userId);

        // 1. Crear la consulta a la SUBCOLECCIÓN 'favoritos' del usuario
        Query query = mFirestore.collection("usuarios").document(userId)
                .collection("favoritos")
                .orderBy("fechaAgregado", Query.Direction.DESCENDING); // Ordenar por más nuevo

        // 2. Configurar las opciones del adaptador
        // Usamos la clase 'Lugar' como molde. ¡DEBEN coincidir los campos!
        FirestoreRecyclerOptions<Lugar> options = new FirestoreRecyclerOptions.Builder<Lugar>()
                .setQuery(query, Lugar.class)
                .build();

        // 3. Crear y configurar el Adaptador
        adapter = new LugarAdapter(options);
        rvFavoritos.setAdapter(adapter);
        adapter.setOnLugarClickListener(this);

        // Empezar a escuchar cambios
        adapter.startListening();

        // También llenamos nuestro Set local para que el ícono (corazón)
        // se muestre correctamente (lleno) en esta pantalla.
        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            userFavoriteLugarIds.clear();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                userFavoriteLugarIds.add(doc.getId());
            }
            Log.d(TAG, "IDs de favoritos locales actualizados: " + userFavoriteLugarIds.size());
            adapter.notifyDataSetChanged(); // Refrescar la UI
        });
    }

    // --- Implementación de los Clics del Adaptador ---

    @Override
    public void onLugarClick(DocumentSnapshot documentSnapshot, int position) {
        // El clic en la tarjeta te lleva a Detalles
        String lugarId = documentSnapshot.getId();
        Intent intent = new Intent(getActivity(), LugarDetailActivity.class);
        intent.putExtra("LUGAR_ID", lugarId);
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(DocumentSnapshot documentSnapshot, int position) {
        // En esta pantalla, un clic en "Favorito" debe ELIMINARLO
        if (currentUser == null) return; // Doble chequeo

        String lugarId = documentSnapshot.getId();

        Log.d(TAG, "Intentando eliminar de favoritos: " + lugarId);

        mFirestore.collection("usuarios").document(currentUser.getUid())
                .collection("favoritos").document(lugarId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    userFavoriteLugarIds.remove(lugarId);
                    Toast.makeText(getContext(), "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
                    // El adaptador de FirebaseUI se actualizará solo
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al eliminar", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error al eliminar favorito: " + e.getMessage());
                });
    }

    @Override
    public boolean isLugarFavorite(DocumentSnapshot documentSnapshot) {
        // En esta pantalla, TODOS los ítems son favoritos
        return userFavoriteLugarIds.contains(documentSnapshot.getId());
        // O simplemente: return true; (Pero es mejor verificar contra el Set)
    }

    // --- Manejo del Ciclo de Vida ---

    @Override
    public void onStart() {
        super.onStart();
        // Es crucial revisar el estado del usuario CADA VEZ que la pestaña se muestra
        currentUser = mAuth.getCurrentUser(); // Refrescar usuario
        checkUserStatus(); // Cargar la UI correcta
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening(); // Dejar de escuchar para ahorrar batería
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}