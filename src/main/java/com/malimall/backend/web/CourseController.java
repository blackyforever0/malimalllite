package com.malimall.backend.web;

import com.malimall.backend.dto.CourseDtos.ChauffeurDisponibleResponse;
import com.malimall.backend.dto.CourseDtos.CourseResponse;
import com.malimall.backend.dto.CourseDtos.EstimationResponse;
import com.malimall.backend.dto.CourseDtos.NoterCourseRequest;
import com.malimall.backend.dto.CourseDtos.TrajetRequest;
import com.malimall.backend.security.UserPrincipal;
import com.malimall.backend.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Module MotoTaxi : demande, acceptation, fin, annulation et notation des courses. */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/quartiers")
    public List<String> quartiers() {
        return courseService.quartiers();
    }

    @PostMapping("/estimation")
    public EstimationResponse estimer(@Valid @RequestBody TrajetRequest req) {
        return courseService.estimer(req.depart(), req.destination(), req.typeVehicule());
    }

    @GetMapping("/chauffeurs-disponibles")
    public List<ChauffeurDisponibleResponse> chauffeursDisponibles() {
        return courseService.chauffeursDisponibles();
    }

    @PostMapping
    public CourseResponse demander(@AuthenticationPrincipal UserPrincipal principal,
                                   @Valid @RequestBody TrajetRequest req) {
        return courseService.demander(principal.getId(), req.depart(), req.destination(), req.typeVehicule());
    }

    /** role=client (défaut) | chauffeur */
    @GetMapping
    public List<CourseResponse> mesCourses(@AuthenticationPrincipal UserPrincipal principal,
                                           @RequestParam(defaultValue = "client") String role) {
        return "chauffeur".equals(role)
                ? courseService.mesCoursesChauffeur(principal.getId())
                : courseService.mesCoursesClient(principal.getId());
    }

    @PreAuthorize("hasRole('CHAUFFEUR')")
    @GetMapping("/disponibles")
    public List<CourseResponse> disponibles(@AuthenticationPrincipal UserPrincipal principal) {
        return courseService.disponibles(principal.getId());
    }

    @GetMapping("/{id}")
    public CourseResponse obtenir(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return courseService.obtenir(id, principal.getId());
    }

    @PreAuthorize("hasRole('CHAUFFEUR')")
    @PostMapping("/{id}/accepter")
    public CourseResponse accepter(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return courseService.accepter(id, principal.getId());
    }

    @PreAuthorize("hasRole('CHAUFFEUR')")
    @PostMapping("/{id}/terminer")
    public CourseResponse terminer(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return courseService.terminer(id, principal.getId());
    }

    @PostMapping("/{id}/annuler")
    public CourseResponse annuler(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return courseService.annuler(id, principal.getId());
    }

    @PostMapping("/{id}/noter")
    public CourseResponse noter(@AuthenticationPrincipal UserPrincipal principal,
                                @PathVariable Long id,
                                @Valid @RequestBody NoterCourseRequest req) {
        return courseService.noter(id, principal.getId(), req.note(), req.commentaire());
    }
}
