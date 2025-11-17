package com.example.turistapp_v1;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LugarDetailActivity extends AppCompatActivity {

    private static final String TAG = "LugarDetailActivity";

    // UI elements
    private ImageView ivImagen;
    private TextView tvNombre, tvDescripcion, tvDireccion, tvRatingValue, tvCantidadRatings;
    private RatingBar ratingBar;
    private ImageButton btnBack;
    private ImageButton btnFavorite; // Botón de favorito

    // Botones de administrador
    private MaterialButton btnEditLugar;   // <-- NUEVO
    private MaterialButton btnDeleteLugar; // <-- NUEVO

    // Firebase
    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    // Data
    private String lugarId;
    private Lugar currentLugar; // Para tener el objeto completo del lugar
    private boolean isFavorite = false; // Estado del favorito

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lugar_detail);

        // --- 1. Inicializar Firebase ---
        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // --- 2. Conectar Vistas ---
        ivImagen = findViewById(R.id.detail_lugar_imagen);
        tvNombre = findViewById(R.id.detail_lugar_nombre);
        tvDescripcion = findViewById(R.id.detail_lugar_descripcion);
        tvDireccion = findViewById(R.id.detail_lugar_direccion);
        tvRatingValue = findViewById(R.id.detail_lugar_rating_value);
        tvCantidadRatings = findViewById(R.id.detail_lugar_cantidad_ratings);
        ratingBar = findViewById(R.id.detail_lugar_rating_bar);
        btnBack = findViewById(R.id.btn_back);
        btnFavorite = findViewById(R.id.btn_favorite_detail);

        btnEditLugar = findViewById(R.id.btn_edit_lugar);     // <-- CONECTAR
        btnDeleteLugar = findViewById(R.id.btn_delete_lugar); // <-- CONECTAR

        // --- 3. Obtener Lugar ID del Intent ---
        if (getIntent().getExtras() != null) {
            lugarId = getIntent().getStringExtra("LUGAR_ID");
            if (lugarId != null) {
                loadLugarDetails();
            } else {
                Toast.makeText(this, "Error: No se especificó el ID del lugar.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "LUGAR_ID es null.");
                finish();
            }
        } else {
            Toast.makeText(this, "Error: No se recibió información del lugar.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No hay extras en el intent.");
            finish();
        }

        // --- 4. Configurar Click Listeners ---
        btnBack.setOnClickListener(v -> {
            // Usar NavUtils para una navegación "Up" correcta que respete el parentActivityName del manifest.
            // Esto asegura que al volver, regrese a MainActivity en lugar de cerrar la app.
            NavUtils.navigateUpFromSameTask(LugarDetailActivity.this);
        });
        btnFavorite.setOnClickListener(v -> toggleFavorite()); // Botón de Favorito

        // Listeners para los nuevos botones de administrador
        btnEditLugar.setOnClickListener(v -> editLugar());     // <-- LISTENERS
        btnDeleteLugar.setOnClickListener(v -> confirmDelete()); // <-- LISTENERS
    }

    private void loadLugarDetails() {
        mFirestore.collection("lugares").document(lugarId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentLugar = documentSnapshot.toObject(Lugar.class);
                        if (currentLugar != null) {
                            displayLugarDetails(currentLugar);
                            checkFavoriteStatus(); // Verificar si es favorito
                            checkUserRoleForAdminButtons(); // <-- Verificar rol para botones admin
                        }
                    } else {
                        Toast.makeText(LugarDetailActivity.this, "Lugar no encontrado.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LugarDetailActivity.this, "Error al cargar detalles.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error al cargar lugar: " + e.getMessage());
                    finish();
                });
    }

    /**
     * Muestra los detalles de un Lugar en la UI.
     */
    private void displayLugarDetails(@NonNull Lugar lugar) {
        tvNombre.setText(lugar.getNombre());
        tvDescripcion.setText(lugar.getDescripcion());
        tvDireccion.setText(lugar.getDireccion());

        if (lugar.getRatingPromedio() > 0) {
            tvRatingValue.setText(String.format("%.1f", lugar.getRatingPromedio()));
            ratingBar.setRating((float) lugar.getRatingPromedio());
            tvCantidadRatings.setText(String.format("(%d ratings)", lugar.getCantidadRatings()));
        } else {
            tvRatingValue.setText("Sin Calificar");
            ratingBar.setRating(0);
            tvCantidadRatings.setText("(0 ratings)");
        }

        if (lugar.getUrlImagen() != null && !lugar.getUrlImagen().isEmpty()) {
            Glide.with(this)
                    .load(lugar.getUrlImagen())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(ivImagen);
        } else {
            ivImagen.setImageResource(R.drawable.placeholder_image);
        }
    }

    private void checkFavoriteStatus() {
        if (currentUser == null) {
            isFavorite = false;
            updateFavoriteIcon();
            return;
        }

        mFirestore.collection("usuarios").document(currentUser.getUid())
                .collection("favoritos").document(lugarId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    isFavorite = documentSnapshot.exists();
                    updateFavoriteIcon();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error al verificar favorito: " + e.getMessage()));
    }

    private void toggleFavorite() {
        if (currentUser == null) {
            Toast.makeText(this, "Debes iniciar sesión para agregar a favoritos.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isFavorite) {
            // Eliminar de favoritos
            mFirestore.collection("usuarios").document(currentUser.getUid())
                    .collection("favoritos").document(lugarId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        isFavorite = false;
                        updateFavoriteIcon();
                        Toast.makeText(this, "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error al eliminar de favoritos: " + e.getMessage()));
        } else {
            // Agregar a favoritos
            if (currentLugar != null) {
                Map<String, Object> favoritoData = new HashMap<>();
                favoritoData.put("lugarId", lugarId);
                favoritoData.put("nombre", currentLugar.getNombre());
                favoritoData.put("urlImagen", currentLugar.getUrlImagen());
                favoritoData.put("direccion", currentLugar.getDireccion());
                favoritoData.put("ratingPromedio", currentLugar.getRatingPromedio());
                favoritoData.put("fechaAgregado", FieldValue.serverTimestamp());

                mFirestore.collection("usuarios").document(currentUser.getUid())
                        .collection("favoritos").document(lugarId)
                        .set(favoritoData)
                        .addOnSuccessListener(aVoid -> {
                            isFavorite = true;
                            updateFavoriteIcon();
                            Toast.makeText(this, "Agregado a favoritos", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "Error al agregar a favoritos: " + e.getMessage()));
            }
        }
    }

    private void updateFavoriteIcon() {
        if (isFavorite) {
            btnFavorite.setImageResource(R.drawable.ic_favorite_filled_24dp);
            btnFavorite.setColorFilter(android.graphics.Color.RED);
        } else {
            btnFavorite.setImageResource(R.drawable.ic_favorite_border_24dp);
            btnFavorite.setColorFilter(android.graphics.Color.parseColor("#999999"));
        }
    }

    // --- NUEVOS MÉTODOS PARA LA LÓGICA DE ADMINISTRADOR ---


    private void checkUserRoleForAdminButtons() {
        if (currentUser == null) {
            btnEditLugar.setVisibility(View.GONE);
            btnDeleteLugar.setVisibility(View.GONE);
            return;
        }

        mFirestore.collection("usuarios").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String rol = documentSnapshot.getString("rol");
                        if ("admin".equals(rol)) {
                            btnEditLugar.setVisibility(View.VISIBLE);
                            btnDeleteLugar.setVisibility(View.VISIBLE);
                        } else {
                            btnEditLugar.setVisibility(View.GONE);
                            btnDeleteLugar.setVisibility(View.GONE);
                        }
                    } else {
                        btnEditLugar.setVisibility(View.GONE);
                        btnDeleteLugar.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al verificar rol de usuario: " + e.getMessage());
                    btnEditLugar.setVisibility(View.GONE);
                    btnDeleteLugar.setVisibility(View.GONE);
                });
    }


    private void editLugar() {
        if (currentLugar == null || lugarId == null) {
            Toast.makeText(this, "Error: No se pudo obtener la información del lugar para editar.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(LugarDetailActivity.this, AddLugarActivity.class);
        intent.putExtra("LUGAR_ID_TO_EDIT", lugarId); // Mandamos el ID del lugar a editar
        startActivity(intent);
    }

    /**
     * Muestra un diálogo de confirmación antes de eliminar el lugar.
     */
    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Lugar")
                .setMessage("¿Estás seguro de que quieres eliminar este lugar? Esta acción no se puede deshacer.")
                .setPositiveButton("Sí, Eliminar", (dialog, which) -> deleteLugar())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Elimina el lugar de Firestore.
     */
    private void deleteLugar() {
        if (lugarId == null) {
            Toast.makeText(this, "Error: No se pudo eliminar el lugar (ID no encontrado).", Toast.LENGTH_SHORT).show();
            return;
        }

        mFirestore.collection("lugares").document(lugarId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(LugarDetailActivity.this, "Lugar eliminado exitosamente.", Toast.LENGTH_SHORT).show();
                    // Opcional: Eliminarlo también de los favoritos de todos los usuarios
                    // (Esto sería más complejo y podría afectar el rendimiento para muchos usuarios)
                    Log.d(TAG, "Lugar eliminado de Firestore: " + lugarId);
                    finish(); // Volver a la actividad anterior
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LugarDetailActivity.this, "Error al eliminar el lugar.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error al eliminar lugar: " + e.getMessage());
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar detalles y estado de favorito cada vez que la actividad vuelve a ser visible
        // por si se editó/eliminó el lugar o cambió el estado de favorito desde AddLugarActivity
        if (lugarId != null) {
            loadLugarDetails();
        }
    }
}
