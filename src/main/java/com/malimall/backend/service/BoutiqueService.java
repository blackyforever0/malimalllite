package com.malimall.backend.service;

import com.malimall.backend.dto.CatalogueDtos.BoutiqueRequest;
import com.malimall.backend.entity.Boutique;
import com.malimall.backend.entity.Vendeur;
import com.malimall.backend.exception.AccesRefuseException;
import com.malimall.backend.exception.RessourceIntrouvableException;
import com.malimall.backend.repository.BoutiqueRepository;
import com.malimall.backend.repository.VendeurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class BoutiqueService {

    private final BoutiqueRepository boutiqueRepository;
    private final VendeurRepository vendeurRepository;
    private final SupabaseStorageService supabaseStorageService;

    public BoutiqueService(BoutiqueRepository boutiqueRepository,
                            VendeurRepository vendeurRepository,
                            SupabaseStorageService supabaseStorageService) {
        this.boutiqueRepository = boutiqueRepository;
        this.vendeurRepository = vendeurRepository;
        this.supabaseStorageService = supabaseStorageService;
    }

    public List<Boutique> lister() {
        return boutiqueRepository.findAll();
    }

    public Boutique obtenir(Long id) {
        return boutiqueRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Boutique introuvable : " + id));
    }

    @Transactional
    public Boutique creer(Long utilisateurId, BoutiqueRequest req) {
        Vendeur vendeur = vendeurRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new IllegalStateException("Il faut être vendeur (POST /api/moi/devenir-vendeur) pour créer une boutique"));

        Boutique boutique = Boutique.builder()
                .proprietaire(vendeur)
                .nom(req.nom())
                .description(req.description())
                .categorie(req.categorie())
                .quartier(req.quartier())
                .certifiee(false)
                .build();
        return boutiqueRepository.save(boutique);
    }

    @Transactional
    public Boutique televerserPhoto(Long utilisateurId, Long boutiqueId, MultipartFile fichier) {
        Boutique boutique = obtenir(boutiqueId);
        Vendeur vendeur = vendeurRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new IllegalStateException("Il faut être vendeur pour modifier une boutique"));
        if (!boutique.getProprietaire().getId().equals(vendeur.getId())) {
            throw new AccesRefuseException("Cette boutique ne vous appartient pas");
        }
        String url = supabaseStorageService.televerser("boutiques", boutique.getId(), fichier);
        boutique.setImageUrl(url);
        return boutiqueRepository.save(boutique);
    }
}
