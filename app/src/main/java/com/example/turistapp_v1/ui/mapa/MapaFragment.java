package com.example.turistapp_v1.ui.mapa;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.turistapp_v1.Lugar;
import com.example.turistapp_v1.LugarDetailActivity;
import com.example.turistapp_v1.R;
import com.example.turistapp_v1.databinding.FragmentMapaBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class MapaFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "MapaFragment";
    private FragmentMapaBinding binding;
    private GoogleMap mMap;
    private FirebaseFirestore mFirestore;
    private Map<String, Lugar> lugaresMap = new HashMap<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMapaBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mFirestore = FirebaseFirestore.getInstance();

        // Obtener el SupportMapFragment y notificar cuando el mapa esté listo
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.map_container, mapFragment)
                    .commit();
        }
        mapFragment.getMapAsync(this);

        return root;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        
        // Configurar el mapa
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        
        // Cargar lugares desde Firestore
        cargarLugaresEnMapa();
        
        // Configurar listener para clics en marcadores
        mMap.setOnMarkerClickListener(marker -> {
            String lugarId = (String) marker.getTag();
            if (lugarId != null) {
                Intent intent = new Intent(getActivity(), LugarDetailActivity.class);
                intent.putExtra("LUGAR_ID", lugarId);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    private void cargarLugaresEnMapa() {
        mFirestore.collection("lugares")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        lugaresMap.clear();
                        if (mMap != null) {
                            mMap.clear();
                        }
                        
                        boolean firstMarker = true;
                        LatLng firstLocation = null;
                        
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Lugar lugar = document.toObject(Lugar.class);
                            if (lugar != null && lugar.getLatitud() != 0 && lugar.getLongitud() != 0) {
                                String lugarId = document.getId();
                                lugar.setId(lugarId);
                                lugaresMap.put(lugarId, lugar);
                                
                                LatLng location = new LatLng(lugar.getLatitud(), lugar.getLongitud());
                                
                                Marker marker = mMap.addMarker(new MarkerOptions()
                                        .position(location)
                                        .title(lugar.getNombre())
                                        .snippet(lugar.getCategoria()));
                                
                                if (marker != null) {
                                    marker.setTag(lugarId);
                                }
                                
                                if (firstMarker) {
                                    firstLocation = location;
                                    firstMarker = false;
                                }
                            }
                        }
                        
                        // Mover la cámara al primer lugar o a Santiago por defecto
                        if (firstLocation != null && mMap != null) {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 10));
                        } else if (mMap != null) {
                            // Ubicación por defecto: Santiago, Chile
                            LatLng santiago = new LatLng(-33.4489, -70.6693);
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(santiago, 10));
                        }
                        
                        Log.d(TAG, "Lugares cargados: " + lugaresMap.size());
                    } else {
                        Log.e(TAG, "Error al cargar lugares", task.getException());
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}