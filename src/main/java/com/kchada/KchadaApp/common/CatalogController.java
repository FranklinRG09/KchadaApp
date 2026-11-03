package com.kchada.KchadaApp.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CatalogController {

    private static final List<String> DEPARTMENTS = List.of(
            "Ahuachapán",
            "Cabañas",
            "Chalatenango",
            "Cuscatlán",
            "La Libertad",
            "La Paz",
            "La Unión",
            "Morazán",
            "San Miguel",
            "San Salvador",
            "San Vicente",
            "Santa Ana",
            "Sonsonate",
            "Usulután"
    );

    private static final List<String> CATEGORIES = List.of(
            "Tecnología",
            "Vehículos",
            "Moda",
            "Hogar",
            "Electrodomésticos",
            "Deportes",
            "Videojuegos",
            "Belleza",
            "Herramientas",
            "Mascotas",
            "Libros",
            "Otros"
    );

    @GetMapping("/api/departments")
    public List<String> departments() {
        return DEPARTMENTS;
    }

    @GetMapping("/api/posts/categories")
    public List<String> categories() {
        return CATEGORIES;
    }
}