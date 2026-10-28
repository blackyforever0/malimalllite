package com.malimall.backend.service;

import java.util.List;
import java.util.Map;

public final class CategoriesCatalogue {

    private static final Map<String, List<String>> SOUS_CATEGORIES = Map.of(
            "Mode", List.of("Vêtements", "Chaussures", "Sacs et accessoires", "Autres articles de mode"),
            "Artisanat", List.of("Poterie", "Tissage", "Vannerie", "Artisanat divers"),
            "Bijoux", List.of("Colliers", "Bracelets", "Bagues", "Boucles d’oreilles", "Bijoux divers"),
            "Maison", List.of("Décoration", "Mobilier", "Cuisine", "Linge de maison", "Autres articles maison"),
            "Beauté", List.of("Soins du visage", "Soins du corps", "Maquillage", "Parfums"),
            "Électronique", List.of("Téléphones et tablettes", "Audio", "Informatique",
                    "Accessoires électroniques", "Électroménager"),
            "Alimentation", List.of("Céréales et légumineuses", "Huiles et condiments", "Boissons",
                    "Produits frais", "Épicerie"));

    private CategoriesCatalogue() {
    }

    public static boolean estCategorieBoutiqueValide(String categorie) {
        return categorie != null && SOUS_CATEGORIES.containsKey(categorie);
    }

    public static List<String> sousCategories(String categorieBoutique) {
        return SOUS_CATEGORIES.getOrDefault(categorieBoutique, List.of());
    }
}
