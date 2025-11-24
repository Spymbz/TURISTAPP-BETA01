package com.example.turistapp_v1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LugarDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ImageView ivImagen;
    private TextView tvNombre, tvDescripcion, tvDireccion, tvHorario, tvCosto;
    private ImageButton btnBack, btnFavorite;
    private MaterialButton btnEditLugar, btnDeleteLugar, btnGoToMap;
    private MapView mapView;
    private GoogleMap googleMap;

    private DatabaseReference mDatabase;
    private FavoritesDBHelper dbHelper;

    private String lugarId;
    private Lugar currentLugar;
    private boolean isFavorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lugar_detail);

        mDatabase = FirebaseDatabase.getInstance().getReference("lugares");
        dbHelper = new FavoritesDBHelper(this);

        ivImagen = findViewById(R.id.detail_lugar_imagen);
        tvNombre = findViewById(R.id.detail_lugar_nombre);
        tvDescripcion = findViewById(R.id.detail_lugar_descripcion);
        tvDireccion = findViewById(R.id.detail_lugar_direccion);
        tvHorario = findViewById(R.id.detail_lugar_horario);
        tvCosto = findViewById(R.id.detail_lugar_costo);
        btnBack = findViewById(R.id.btn_back);
        btnFavorite = findViewById(R.id.btn_favorite_detail);
        btnEditLugar = findViewById(R.id.btn_edit_lugar);
        btnDeleteLugar = findViewById(R.id.btn_delete_lugar);
        mapView = findViewById(R.id.map_view_detail);
        btnGoToMap = findViewById(R.id.btn_go_to_map);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        lugarId = getIntent().getStringExtra("LUGAR_ID");

        if (lugarId != null) {
            loadLugarDetails();
        } else {
            Toast.makeText(this, "Error: ID del lugar no encontrado.", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnBack.setOnClickListener(v -> finish());
        btnFavorite.setOnClickListener(v -> toggleFavorite());
        btnGoToMap.setOnClickListener(v -> navigateToMapFragment());

        btnEditLugar.setVisibility(View.GONE);
        btnDeleteLugar.setVisibility(View.GONE);
    }

    private void loadLugarDetails() {
        mDatabase.child(lugarId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentLugar = snapshot.getValue(Lugar.class);
                    if (currentLugar != null) {
                        displayLugarDetails(currentLugar);
                        checkFavoriteStatus();
                        if (googleMap != null) {
                            initMap(currentLugar);
                        }
                    }
                } else {
                    Toast.makeText(LugarDetailActivity.this, "Lugar no encontrado.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LugarDetailActivity.this, "Error al cargar detalles.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayLugarDetails(@NonNull Lugar lugar) {
        tvNombre.setText(lugar.getNombre());
        tvDescripcion.setText(lugar.getDescripcion());
        tvDireccion.setText(lugar.getDireccion());

        if (lugar.getUrlImagen() != null && !lugar.getUrlImagen().isEmpty()) {
            Glide.with(this).load(lugar.getUrlImagen()).into(ivImagen);
        }

        if (lugar.getHorario() != null && !lugar.getHorario().isEmpty()) {
            tvHorario.setText(lugar.getHorario());
        } else {
            tvHorario.setText("No especificado");
        }

        if (lugar.getCostoAproximado() != null) {
            tvCosto.setText("$" + lugar.getCostoAproximado());
        } else {
            tvCosto.setText("No especificado");
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.getUiSettings().setMapToolbarEnabled(false);
        if (currentLugar != null) {
            initMap(currentLugar);
        }
    }

    private void initMap(@NonNull Lugar lugar) {
        if (lugar.getLatitud() != null && lugar.getLongitud() != null) {
            LatLng lugarPosition = new LatLng(lugar.getLatitud(), lugar.getLongitud());
            googleMap.addMarker(new MarkerOptions().position(lugarPosition).title(lugar.getNombre()));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lugarPosition, 15f));
        }
    }

    private void navigateToMapFragment() {
        if (currentLugar != null && currentLugar.getLatitud() != null && currentLugar.getLongitud() != null) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("NAVIGATE_TO_MAP", true);
            intent.putExtra("lugar_lat", currentLugar.getLatitud());
            intent.putExtra("lugar_lng", currentLugar.getLongitud());
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Ubicación no disponible.", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkFavoriteStatus() {
        isFavorite = dbHelper.isFavorite(lugarId);
        updateFavoriteIcon();
    }

    private void toggleFavorite() {
        if (isFavorite) {
            dbHelper.removeFavorite(lugarId);
            Toast.makeText(this, "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
        } else {
            dbHelper.addFavorite(currentLugar);
            Toast.makeText(this, "Agregado a favoritos", Toast.LENGTH_SHORT).show();
        }
        isFavorite = !isFavorite;
        updateFavoriteIcon();
    }

    private void updateFavoriteIcon() {
        if (isFavorite) {
            btnFavorite.setImageResource(R.drawable.ic_favorite_filled_24dp);
            btnFavorite.setColorFilter(getResources().getColor(android.R.color.holo_red_light));
        } else {
            btnFavorite.setImageResource(R.drawable.ic_favorite_border_24dp);
            btnFavorite.setColorFilter(getResources().getColor(android.R.color.darker_gray));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        mapView.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
