package com.geobook;

public class TestOrdImage {
    public static void main(String[] args) {
        try {
            Class<?> clazz = Class.forName("oracle.ord.im.OrdImage");
            System.out.println("OrdImage class loaded: " + clazz.getName());
        } catch (ClassNotFoundException e) {
            System.out.println("OrdImage class not found: " + e.getMessage());
        }
    }
}
