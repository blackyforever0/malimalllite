package com.malimall.backend.service;

import com.malimall.backend.dto.ProfilDtos.DevenirChauffeurRequest;
import com.malimall.backend.entity.Chauffeur;
import com.malimall.backend.entity.Utilisateur;
import com.malimall.backend.entity.Vehicule;
import com.malimall.backend.entity.Vendeur;
import com.malimall.backend.exception.RessourceIntrouvableException;
import com.malimall.backend.repository.ChauffeurRepository;
import com.malimall.backend.repository.UtilisateurRepository;
import com.malimall.backend.repository.VehiculeRepository;
import com.malimall.backend.repository.VendeurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfilService {

    private final UtilisateurRepository utilisateurRepository;
    private final VendeurRepository vendeurRepository;
    private final ChauffeurRepository chauffeurRepository;
    private final VehiculeRepository vehiculeRepository;
    private final SupabaseStorageService supabaseStorageService;

    public ProfilService(UtilisateurRepository utilisateurRepository,
                          VendeurRepository vendeurRepository,
                          ChauffeurRepository chauffeurRepository,
                          VehiculeRepository vehiculeRepository,
                          SupabaseStorageService supabaseStorageService) {
        this.utilisateurRepository = utilisateurRepository;
        this.vendeurRepository = vendeurRepository;
        this.chauffeurRepository = chauffeurRepository;
        this.vehiculeRepository = vehiculeRepository;
        this.supabaseStorageService = supabaseStorageService;
    }

    @Transactional
    public Vendeur devenirVendeur(Long utilisateurId) {
        if (vendeurRepository.findByUtilisateurId(utilisateurId).isPresent()) {
            throw new IllegalStateException("Cet utilisateur est déjà vendeur");
        }
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable"));
        return vendeurRepository.save(Vendeur.builder().utilisateur(utilisateur).build());
    }

    @Transactional
    public Chauffeur devenirChauffeur(Long utilisateurId, DevenirChauffeurRequest req) {
        if (chauffeurRepository.findByUtilisateurId(utilisateurId).isPresent()) {
            throw new IllegalStateException("Cet utilisateur est déjà chauffeur");
        }
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable"));

        Chauffeur chauffeur = chauffeurRepository.save(
                Chauffeur.builder()
                        .utilisateur(utilisateur)
                        .disponible(true)
                        .numeroPermis(req.numeroPermis())
                        .build());

        Vehicule vehicule = Vehicule.builder()
                .chauffeur(chauffeur)
                .type(req.typeVehicule())
                .immatriculation(req.immatriculation())
                .build();
        vehiculeRepository.save(vehicule);

        return chauffeur;
    }

    @Transactional
    public Utilisateur televerserPhoto(Long utilisateurId, MultipartFile fichier) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable"));
        String url = supabaseStorageService.televerser("utilisateurs", utilisateurId, fichier);
        utilisateur.setImageUrl(url);
        return utilisateurRepository.save(utilisateur);
    }
}
