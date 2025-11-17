package com.example.turistapp_v1;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter; // Importar
import android.widget.Button; // Importar
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView; // Importar
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot; // Importar
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AddLugarActivity extends AppCompatActivity {

    private static final String TAG = "AddLugarActivity";

    // --- Variables de la UI ---
    ImageButton btnBack;
    TextView tvTitle; // Título de la actividad
    TextInputEditText etNombreLugar, etDescLugar, etLatitud, etLongitud, etDireccion;
    MaterialButton btnSeleccionarImagen, btnGuardarLugar;
    ImageView ivImagenSeleccionada;
    Spinner spinnerRegion, spinnerCategoria;

    // --- NUEVO: Variables de estado ---
    private boolean isEditing = false; // ¿Estamos en modo edición?
    private String editingLugarId = null; // ID del lugar que estamos editando
    private String existingImageUrl = null; // URL de la imagen actual (para no cambiarla si no se selecciona una nueva)

    // --- Variables de Firebase ---
    FirebaseAuth mAuth;
    FirebaseFirestore mFirestore;
    FirebaseStorage mStorage;

    // --- NUEVO: Spinners Adapters (para preseleccionar valores) ---
    ArrayAdapter<String> regionAdapter;
    ArrayAdapter<String> categoriaAdapter;

    private Uri selectedImageUri;

    private ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Glide.with(this).load(uri).into(ivImagenSeleccionada);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_lugar);

        // --- Inicializar Firebase ---
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mStorage = FirebaseStorage.getInstance();

        // --- Conectar variables con el XML ---
        btnBack = findViewById(R.id.btn_back_add_lugar);
        tvTitle = findViewById(R.id.tv_title_add_lugar); // Conectar el Título
        etNombreLugar = findViewById(R.id.et_nombre_lugar);
        etDescLugar = findViewById(R.id.et_desc_lugar);
        ivImagenSeleccionada = findViewById(R.id.iv_imagen_seleccionada);
        btnSeleccionarImagen = findViewById(R.id.btn_seleccionar_imagen);
        spinnerRegion = findViewById(R.id.spinner_region);
        spinnerCategoria = findViewById(R.id.spinner_categoria);
        etLatitud = findViewById(R.id.et_latitud);
        etLongitud = findViewById(R.id.et_longitud);
        etDireccion = findViewById(R.id.et_direccion);
        btnGuardarLugar = findViewById(R.id.btn_guardar_lugar);

        // --- Configurar Spinners ---
        setupSpinners();

        // --- NUEVO: Verificar si estamos en Modo Edición ---
        if (getIntent().getExtras() != null) {
            editingLugarId = getIntent().getStringExtra("LUGAR_ID_TO_EDIT");
            if (editingLugarId != null && !editingLugarId.isEmpty()) {
                isEditing = true;
                setupEditMode();
            }
        }

        // --- Configurar Click Listeners ---
        btnBack.setOnClickListener(v -> finish());
        btnSeleccionarImagen.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnGuardarLugar.setOnClickListener(v -> validateAndSaveLugar());
    }

    private void setupSpinners() {
        String[] regionesChile = {
                "Seleccione una Región", "Arica y Parinacota", "Tarapacá", "Antofagasta",
                "Atacama", "Coquimbo", "Valparaíso", "Metropolitana", "O'Higgins",
                "Maule", "Ñuble", "Biobío", "La Araucanía", "Los Ríos", "Los Lagos", "Aysén", "Magallanes"
        };
        regionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, regionesChile);
        regionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRegion.setAdapter(regionAdapter);

        String[] categoriasLugares = {
                "Seleccione una Categoría", "Playas", "Parques Nacionales", "Monumentos", "Museos",
                "Miradores", "Termas", "Iglesias", "Plazas/Mercados", "Viñas"
        };
        categoriaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoriasLugares);
        categoriaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoria.setAdapter(categoriaAdapter);
    }

    private void setupEditMode() {
        tvTitle.setText("Editar Lugar");
        btnGuardarLugar.setText("Guardar Cambios");
        loadLugarData();
    }

    private void loadLugarData() {
        mFirestore.collection("lugares").document(editingLugarId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Lugar lugar = documentSnapshot.toObject(Lugar.class);
                        if (lugar != null) {
                            etNombreLugar.setText(lugar.getNombre());
                            etDescLugar.setText(lugar.getDescripcion());
                            etLatitud.setText(String.valueOf(lugar.getLatitud()));
                            etLongitud.setText(String.valueOf(lugar.getLongitud()));
                            etDireccion.setText(lugar.getDireccion());

                            // Guardar la URL de la imagen existente
                            existingImageUrl = lugar.getUrlImagen();
                            if (existingImageUrl != null && !existingImageUrl.isEmpty()) {
                                Glide.with(this).load(existingImageUrl).into(ivImagenSeleccionada);
                            }

                            // Pre-seleccionar los Spinners
                            int regionPosition = regionAdapter.getPosition(lugar.getRegion());
                            if (regionPosition >= 0) {
                                spinnerRegion.setSelection(regionPosition);
                            }

                            int categoriaPosition = categoriaAdapter.getPosition(lugar.getCategoria());
                            if (categoriaPosition >= 0) {
                                spinnerCategoria.setSelection(categoriaPosition);
                            }
                        }
                    } else {
                        Toast.makeText(this, "Error: No se encontró el lugar a editar.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al cargar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    /**
     * Valida los campos y decide si crear o actualizar.
     */
    private void validateAndSaveLugar() {
        // --- 1. Obtener y Validar Datos ---
        String nombre = etNombreLugar.getText().toString().trim();
        String descripcion = etDescLugar.getText().toString().trim();
        String region = spinnerRegion.getSelectedItem().toString();
        String categoria = spinnerCategoria.getSelectedItem().toString();
        String latitudStr = etLatitud.getText().toString().trim();
        String longitudStr = etLongitud.getText().toString().trim();
        String direccion = etDireccion.getText().toString().trim();

        if (TextUtils.isEmpty(nombre)) { etNombreLugar.setError("Nombre requerido"); etNombreLugar.requestFocus(); return; }
        if (TextUtils.isEmpty(descripcion)) { etDescLugar.setError("Descripción requerida"); etDescLugar.requestFocus(); return; }
        if (region.equals("Seleccione una Región")) { Toast.makeText(this, "Seleccione una región.", Toast.LENGTH_SHORT).show(); return; }
        if (categoria.equals("Seleccione una Categoría")) { Toast.makeText(this, "Seleccione una categoría.", Toast.LENGTH_SHORT).show(); return; }
        if (TextUtils.isEmpty(latitudStr)) { etLatitud.setError("Latitud requerida"); etLatitud.requestFocus(); return; }
        if (TextUtils.isEmpty(longitudStr)) { etLongitud.setError("Longitud requerida"); etLongitud.requestFocus(); return; }
        if (TextUtils.isEmpty(direccion)) { etDireccion.setError("Dirección requerida"); etDireccion.requestFocus(); return; }

        // Si estamos creando (no editando) Y no se seleccionó imagen, es un error.
        if (!isEditing && selectedImageUri == null) {
            Toast.makeText(this, "Debe seleccionar una imagen.", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- 2. Decidir cómo manejar la imagen ---
        Toast.makeText(this, "Guardando lugar...", Toast.LENGTH_LONG).show();

        if (selectedImageUri != null) {
            // Caso A: El usuario seleccionó una NUEVA imagen (para crear o editar)
            // Subimos la nueva imagen
            uploadImageAndSaveData(selectedImageUri);
        } else if (isEditing) {
            // Caso B: El usuario está EDITANDO pero NO seleccionó una nueva imagen
            // Usamos la URL de la imagen existente
            saveDataToFirestore(existingImageUrl);
        }
    }

    private void uploadImageAndSaveData(Uri imageUri) {
        String imageName = UUID.randomUUID().toString() + ".jpg";
        StorageReference imageRef = mStorage.getReference().child("imagenes_lugares/" + imageName);

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Imagen subida, obtener URL
                    imageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        String urlImagen = downloadUri.toString();
                        saveDataToFirestore(urlImagen); // Guardar datos con la NUEVA URL
                    }).addOnFailureListener(e -> {
                        Toast.makeText(AddLugarActivity.this, "Error al obtener URL de imagen.", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error al obtener URL", e);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddLugarActivity.this, "Error al subir la imagen.", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error al subir imagen", e);
                });
    }

    private void saveDataToFirestore(String urlImagen) {
        String adminId = mAuth.getCurrentUser().getUid();

        Map<String, Object> lugarData = new HashMap<>();
        lugarData.put("nombre", etNombreLugar.getText().toString().trim());
        lugarData.put("descripcion", etDescLugar.getText().toString().trim());
        lugarData.put("region", spinnerRegion.getSelectedItem().toString());
        lugarData.put("categoria", spinnerCategoria.getSelectedItem().toString());
        lugarData.put("latitud", Double.parseDouble(etLatitud.getText().toString().trim()));
        lugarData.put("longitud", Double.parseDouble(etLongitud.getText().toString().trim()));
        lugarData.put("direccion", etDireccion.getText().toString().trim());
        lugarData.put("urlImagen", urlImagen); // Usar la URL (nueva o existente)

        if (isEditing) {
            // --- MODO ACTUALIZAR ---
            // No actualizamos la fecha de creación ni el creador
            mFirestore.collection("lugares").document(editingLugarId)
                    .update(lugarData) // .update() solo cambia los campos en el Map
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AddLugarActivity.this, "¡Lugar actualizado!", Toast.LENGTH_SHORT).show();
                        finish(); // Volver a la pantalla de detalles
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddLugarActivity.this, "Error al actualizar.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error al actualizar", e);
                    });
        } else {
            // --- MODO CREAR ---
            // Añadimos los campos que solo van en la creación
            lugarData.put("ratingPromedio", 0.0);
            lugarData.put("cantidadRatings", 0);
            lugarData.put("creadoPorAdminId", adminId);
            lugarData.put("fechaCreacion", FieldValue.serverTimestamp());

            mFirestore.collection("lugares")
                    .add(lugarData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(AddLugarActivity.this, "¡Lugar guardado!", Toast.LENGTH_SHORT).show();
                        finish(); // Volver
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddLugarActivity.this, "Error al guardar.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error al guardar", e);
                    });
        }
    }
}