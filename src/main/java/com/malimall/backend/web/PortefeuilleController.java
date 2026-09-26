package com.malimall.backend.web;

import com.malimall.backend.dto.PortefeuilleDtos.EcritureResponse;
import com.malimall.backend.dto.PortefeuilleDtos.PortefeuilleResponse;
import com.malimall.backend.dto.PortefeuilleDtos.RechargeRequest;
import com.malimall.backend.dto.PortefeuilleDtos.RetraitRequest;
import com.malimall.backend.dto.PortefeuilleDtos.TransfertRequest;
import com.malimall.backend.dto.PortefeuilleDtos.TransfertResponse;
import com.malimall.backend.mapper.PortefeuilleMapper;
import com.malimall.backend.security.UserPrincipal;
import com.malimall.backend.service.PortefeuilleService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Recharge/retrait/transfert — le paiement mobile money (Orange Money, Moov
 * Money, Wave, Sama Money) est simulé côté mobile : aucune de ces routes
 * n'appelle un opérateur réel, elles ne font que mettre à jour le
 * portefeuille MaliMall lui-même (voir PortefeuilleService).
 */
@RestController
@RequestMapping("/api/portefeuille")
public class PortefeuilleController {

    private final PortefeuilleService portefeuilleService;

    public PortefeuilleController(PortefeuilleService portefeuilleService) {
        this.portefeuilleService = portefeuilleService;
    }

    @GetMapping
    public PortefeuilleResponse solde(@AuthenticationPrincipal UserPrincipal principal) {
        return PortefeuilleMapper.toResponse(portefeuilleService.obtenirPourUtilisateur(principal.getId()));
    }

    @GetMapping("/historique")
    public List<EcritureResponse> historique(@AuthenticationPrincipal UserPrincipal principal) {
        return portefeuilleService.historiquePourUtilisateur(principal.getId()).stream()
                .map(PortefeuilleMapper::toResponse)
                .toList();
    }

    @PostMapping("/recharge")
    public PortefeuilleResponse recharger(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody RechargeRequest req) {
        return PortefeuilleMapper.toResponse(portefeuilleService.recharger(principal.getId(), req.montantMmc()));
    }

    @PostMapping("/retrait")
    public PortefeuilleResponse retirer(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody RetraitRequest req) {
        return PortefeuilleMapper.toResponse(portefeuilleService.retirer(principal.getId(), req.montantMmc()));
    }

    @PostMapping("/transfert")
    public TransfertResponse transferer(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody TransfertRequest req) {
        var destinataire = portefeuilleService.trouverParTelephone(req.telephoneDestinataire());
        var portefeuille = portefeuilleService.transferer(principal.getId(), req.telephoneDestinataire(), req.montantMmc());
        return new TransfertResponse(PortefeuilleMapper.toResponse(portefeuille), destinataire.getNom() + " " + destinataire.getPrenom());
    }
}
