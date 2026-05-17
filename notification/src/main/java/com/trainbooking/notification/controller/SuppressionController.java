package com.trainbooking.notification.controller;

import com.trainbooking.notification.domain.entity.SuppressionEntry;
import com.trainbooking.notification.dto.request.AddSuppressionRequest;
import com.trainbooking.notification.dto.response.ApiResponse;
import com.trainbooking.notification.service.SuppressionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/suppressions")
public class SuppressionController {

    private final SuppressionService suppressionService;

    public SuppressionController(SuppressionService suppressionService) {
        this.suppressionService = suppressionService;
    }

    @GetMapping
    public ResponseEntity<List<SuppressionEntry>> list() {
        return ResponseEntity.ok(suppressionService.list());
    }

    @PostMapping
    public ResponseEntity<SuppressionEntry> add(@Valid @RequestBody AddSuppressionRequest request) {
        SuppressionEntry entry = suppressionService.add(
                request.getChannel(),
                request.getRecipient(),
                request.getReason(),
                "MANUAL",
                request.getNotes());
        return ResponseEntity.status(HttpStatus.CREATED).body(entry);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> remove(@PathVariable Long id) {
        suppressionService.remove(id);
        return ResponseEntity.ok(ApiResponse.success("Suppression entry removed"));
    }
}
