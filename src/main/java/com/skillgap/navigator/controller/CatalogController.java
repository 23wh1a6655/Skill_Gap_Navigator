package com.skillgap.navigator.controller;

import com.skillgap.navigator.dto.RoleResponse;
import com.skillgap.navigator.dto.RoleComparisonResponse;
import com.skillgap.navigator.service.CatalogService;
import com.skillgap.navigator.service.NavigatorService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog")
@CrossOrigin
public class CatalogController {

    private final CatalogService catalogService;
    private final NavigatorService navigatorService;

    public CatalogController(CatalogService catalogService, NavigatorService navigatorService) {
        this.catalogService = catalogService;
        this.navigatorService = navigatorService;
    }

    @GetMapping("/roles")
    public List<RoleResponse> getRoles() {
        return catalogService.getRoles();
    }

    @GetMapping("/roles/search")
    public List<RoleResponse> searchRoles(@RequestParam String query) {
        return catalogService.searchRoles(query);
    }

    @GetMapping("/roles/preview")
    public RoleResponse previewRole(@RequestParam String name) {
        return catalogService.previewRoleLive(name)
                .or(() -> catalogService.findRole(name).map(catalogService::toRoleResponse))
                .orElseThrow();
    }

    @GetMapping("/roles/compare")
    public RoleComparisonResponse compareRoles(@RequestParam String primaryRole, @RequestParam String compareRole) {
        return navigatorService.compareRoles(primaryRole, compareRole);
    }
}
