package com.example.turistapp_v1.ui.cuenta;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.turistapp_v1.AddLugarActivity;
import com.example.turistapp_v1.LoginActivity;
import com.example.turistapp_v1.SplashActivity;
import com.example.turistapp_v1.databinding.FragmentCuentaBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class CuentaFragment extends Fragment {

    private static final String TAG = "CuentaFragment";

    private FragmentCuentaBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCuentaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        // Listener para el botón de ir a Login
        binding.btnGoToLoginCuenta.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
        });

        // Listener para el botón de añadir lugar (admin)
        binding.btnAddLugarAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddLugarActivity.class);
            startActivity(intent);
        });

        // Listener para el botón de cerrar sesión
        binding.btnLogout.setOnClickListener(v -> cerrarSesion());
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        checkUserStatus(currentUser);
    }

    private void checkUserStatus(FirebaseUser currentUser) {
        if (currentUser == null) {
            // --- MODO INVITADO ---
            binding.layoutLoginPromptCuenta.setVisibility(View.VISIBLE);
            binding.tvSaludoCuenta.setVisibility(View.GONE);
            binding.btnLogout.setVisibility(View.GONE);
            binding.btnAddLugarAdmin.setVisibility(View.GONE);
        } else {
            // --- USUARIO LOGUEADO ---
            binding.layoutLoginPromptCuenta.setVisibility(View.GONE);
            binding.tvSaludoCuenta.setVisibility(View.VISIBLE);
            binding.btnLogout.setVisibility(View.VISIBLE);
            cargarDatosUsuario(currentUser.getUid());
        }
    }

    private void cargarDatosUsuario(String userId) {
        mFirestore.collection("usuarios").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String nombre = document.getString("nombre");
                            binding.tvSaludoCuenta.setText("¡Hola, " + nombre + "!");

                            String rol = document.getString("rol");
                            if ("admin".equals(rol)) {
                                Log.d(TAG, "El usuario es ADMIN. Mostrando botón.");
                                binding.btnAddLugarAdmin.setVisibility(View.VISIBLE);
                            } else {
                                Log.d(TAG, "El usuario es normal. Botón de admin oculto.");
                                binding.btnAddLugarAdmin.setVisibility(View.GONE);
                            }
                        } else {
                            binding.tvSaludoCuenta.setText("¡Bienvenido!");
                            binding.btnAddLugarAdmin.setVisibility(View.GONE);
                        }
                    } else {
                        binding.tvSaludoCuenta.setText("¡Bienvenido!");
                        binding.btnAddLugarAdmin.setVisibility(View.GONE);
                        Log.e(TAG, "Error al cargar datos", task.getException());
                    }
                });
    }

    private void cerrarSesion() {
        mAuth.signOut();
        Toast.makeText(getActivity(), "Sesión cerrada", Toast.LENGTH_SHORT).show();

        // Reiniciar el estado de la vista al de "no logueado"
        checkUserStatus(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}