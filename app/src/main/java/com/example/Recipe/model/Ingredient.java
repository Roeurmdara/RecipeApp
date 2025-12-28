package com.example.Recipe.model;

public class Ingredient {
    private String name;
    private String quantity;

    public Ingredient() {} // Required for Firebase

    public Ingredient(String name, String quantity) {
        this.name = name;
        this.quantity = quantity;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }
}