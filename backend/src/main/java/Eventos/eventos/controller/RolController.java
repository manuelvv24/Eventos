package Eventos.eventos.controller;

import Eventos.eventos.entity.Rol;
import Eventos.eventos.repository.RolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
public class RolController {

    @Autowired
    private RolRepository rolRepository;
    @GetMapping
    public ResponseEntity<List<Rol>> listar() {
        return ResponseEntity.ok(rolRepository.findByActivoTrueOrderByNombreRolAsc());
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        return rolRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Rol rol) {
        try {
            if (rol.getNombreRol() == null || rol.getNombreRol().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El nombre del rol es obligatorio"));
            }
            if (rolRepository.existsByNombreRolIgnoreCase(rol.getNombreRol())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Ya existe un rol con el nombre: " + rol.getNombreRol()));
            }
            Rol guardado = rolRepository.save(rol);
            return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Rol rolActualizado) {
        return rolRepository.findById(id)
                .map(rol -> {
                    rol.setNombreRol(rolActualizado.getNombreRol());
                    rol.setDescripcion(rolActualizado.getDescripcion());
                    return ResponseEntity.ok(rolRepository.save(rol));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        return rolRepository.findById(id)
                .map(rol -> {
                    if (!Boolean.TRUE.equals(rol.getActivo())) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", "El rol ya está inactivado"));
                    }
                    rol.setActivo(false);
                    rolRepository.save(rol);
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
