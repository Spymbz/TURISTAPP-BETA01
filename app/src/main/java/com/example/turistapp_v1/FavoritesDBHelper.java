package com.example.turistapp_v1;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class FavoritesDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "favorites.db";

    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_FAVORITES = "favorites";

    // Columnas para guardar el objeto Lugar completo
    public static final String COLUMN_LUGAR_ID = "lugar_id";
    public static final String COLUMN_NOMBRE = "nombre";
    public static final String COLUMN_DESCRIPCION = "descripcion";
    public static final String COLUMN_URL_IMAGEN = "url_imagen";
    public static final String COLUMN_REGION = "region";
    public static final String COLUMN_CATEGORIA = "categoria";
    public static final String COLUMN_LATITUD = "latitud";
    public static final String COLUMN_LONGITUD = "longitud";
    public static final String COLUMN_DIRECCION = "direccion";
    public static final String COLUMN_FECHA_CREACION = "fecha_creacion";
    public static final String COLUMN_COSTO_APROXIMADO = "costo_aproximado";
    public static final String COLUMN_HORARIO = "horario";

    // Script para crear la nueva tabla con todos los campos.
    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_FAVORITES + " (" +
                    COLUMN_LUGAR_ID + " TEXT PRIMARY KEY, " +
                    COLUMN_NOMBRE + " TEXT, " +
                    COLUMN_DESCRIPCION + " TEXT, " +
                    COLUMN_URL_IMAGEN + " TEXT, " +
                    COLUMN_REGION + " TEXT, " +
                    COLUMN_CATEGORIA + " TEXT, " +
                    COLUMN_LATITUD + " REAL, " +
                    COLUMN_LONGITUD + " REAL, " +
                    COLUMN_DIRECCION + " TEXT, " +
                    COLUMN_FECHA_CREACION + " INTEGER, " +
                    COLUMN_COSTO_APROXIMADO + " INTEGER, " +
                    COLUMN_HORARIO + " TEXT" +
                    ");";

    public FavoritesDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Elimina la tabla vieja y crea la nueva. Se perderán los favoritos anteriores.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
        onCreate(db);
    }

    public void addFavorite(Lugar lugar) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LUGAR_ID, lugar.getId());
        values.put(COLUMN_NOMBRE, lugar.getNombre());
        values.put(COLUMN_DESCRIPCION, lugar.getDescripcion());
        values.put(COLUMN_URL_IMAGEN, lugar.getUrlImagen());
        values.put(COLUMN_REGION, lugar.getRegion());
        values.put(COLUMN_CATEGORIA, lugar.getCategoria());
        values.put(COLUMN_LATITUD, lugar.getLatitud());
        values.put(COLUMN_LONGITUD, lugar.getLongitud());
        values.put(COLUMN_DIRECCION, lugar.getDireccion());
        values.put(COLUMN_FECHA_CREACION, lugar.getFechaCreacion());
        values.put(COLUMN_COSTO_APROXIMADO, lugar.getCostoAproximado());
        values.put(COLUMN_HORARIO, lugar.getHorario());
        db.insert(TABLE_FAVORITES, null, values);
        db.close();
    }

    public void removeFavorite(String lugarId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_FAVORITES, COLUMN_LUGAR_ID + " = ?", new String[]{lugarId});
        db.close();
    }

    public boolean isFavorite(String lugarId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_FAVORITES,
                new String[]{COLUMN_LUGAR_ID},
                COLUMN_LUGAR_ID + " = ?",
                new String[]{lugarId},
                null, null, null);
        boolean isFav = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return isFav;
    }


    public List<Lugar> getAllFavoriteLugares() {
        List<Lugar> favoriteLugares = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        // Usamos un try-with-resources para asegurar que el cursor se cierre.
        try (Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_FAVORITES, null)) {
            if (cursor.moveToFirst()) {
                do {
                    Lugar lugar = new Lugar();
                    lugar.setId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LUGAR_ID)));
                    lugar.setNombre(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOMBRE)));
                    lugar.setDescripcion(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPCION)));
                    lugar.setUrlImagen(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_URL_IMAGEN)));
                    lugar.setRegion(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REGION)));
                    lugar.setCategoria(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORIA)));
                    lugar.setLatitud(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUD)));
                    lugar.setLongitud(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUD)));
                    lugar.setDireccion(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DIRECCION)));
                    lugar.setFechaCreacion(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_FECHA_CREACION)));
                    lugar.setCostoAproximado(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COSTO_APROXIMADO)));
                    lugar.setHorario(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HORARIO)));
                    favoriteLugares.add(lugar);
                } while (cursor.moveToNext());
            }
        }
        db.close();
        return favoriteLugares;
    }
}
