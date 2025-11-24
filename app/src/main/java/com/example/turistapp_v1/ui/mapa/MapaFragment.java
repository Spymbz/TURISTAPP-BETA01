package com.example.turistapp_v1.ui.mapa;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MapaFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "MapaFragment";
    private FragmentMapaBinding binding;
    private GoogleMap mMap;
    private DatabaseReference mDatabase;
    private Map<String, Lugar> lugaresMap = new HashMap<>();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMapaBinding.inflate(inflater, container, false);
        mDatabase = FirebaseDatabase.getInstance().getReference("lugares");

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupSearch();

        return binding.getRoot();
    }

    private void setupSearch() {
        binding.mapSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchOnMap(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private void searchOnMap(String query) {
        String normalizedQuery = query.toLowerCase(Locale.getDefault()).trim();
        if (normalizedQuery.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Lugar> entry : lugaresMap.entrySet()) {
            Lugar lugar = entry.getValue();
            if (lugar.getNombre().toLowerCase(Locale.getDefault()).contains(normalizedQuery)) {
                LatLng location = new LatLng(lugar.getLatitud(), lugar.getLongitud());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));

                // Ocultar teclado
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(binding.mapSearchView.getWindowToken(), 0);
                binding.mapSearchView.clearFocus();
                return;
            }
        }
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // 1. Coordenadas por defecto: Santiago de Chile
        LatLng santiago = new LatLng(-33.4489, -70.6693);
        LatLng initialPosition = santiago;

        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey("lugar_lat") && arguments.containsKey("lugar_lng")) {
            double lat = arguments.getDouble("lugar_lat");
            double lng = arguments.getDouble("lugar_lng");
            if (lat != 0 || lng != 0) {
                initialPosition = new LatLng(lat, lng);
            }
        }
        
        float initialZoom = (initialPosition == santiago) ? 10f : 15f;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPosition, initialZoom));

        cargarLugaresEnMapa();

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
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                lugaresMap.clear();
                if (mMap != null) mMap.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Lugar lugar = snapshot.getValue(Lugar.class);
                    String lugarId = snapshot.getKey();

                    if (lugar != null && lugar.getLatitud() != null && lugar.getLatitud() != 0 && lugar.getLongitud() != null && lugar.getLongitud() != 0) {
                        lugar.setId(lugarId);
                        lugaresMap.put(lugarId, lugar);

                        LatLng location = new LatLng(lugar.getLatitud(), lugar.getLongitud());
                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(location)
                                .title(lugar.getNombre())
                                .snippet(lugar.getCategoria()));

                        if (marker != null) marker.setTag(lugarId);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error al cargar lugares", databaseError.toException());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
