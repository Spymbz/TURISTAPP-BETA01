package com.example.turistapp_v1;

public class Category {
    private String name;
    private int imageResId;

    public Category() {
        // Constructor vacío requerido por Firebase
    }

    public Category(String name, int imageResId) {
        this.name = name;
        this.imageResId = imageResId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getImageResId() {
        return imageResId;
    }

    public void setImageResId(int imageResId) {
        this.imageResId = imageResId;
    }
}