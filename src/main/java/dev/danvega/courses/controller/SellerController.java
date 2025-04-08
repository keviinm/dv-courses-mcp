package dev.danvega.courses.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.danvega.courses.model.Seller;
import dev.danvega.courses.service.SellerService;

@RestController
@RequestMapping("/api/sellers")
public class SellerController {

    private final SellerService sellerService;

    public SellerController(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    @GetMapping
    public List<Seller> getAllSellers() {
        return sellerService.getAllSellers();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Seller> getSeller(@PathVariable String id) {
        Seller seller = sellerService.getSeller(id);
        return seller != null ? ResponseEntity.ok(seller) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Seller> addSeller(@RequestBody Seller seller) {
        Seller newSeller = sellerService.addSeller(seller);
        return ResponseEntity.status(HttpStatus.CREATED).body(newSeller);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Seller> updateSeller(@PathVariable String id, @RequestBody Seller seller) {
        Seller existingSeller = sellerService.getSeller(id);
        if (existingSeller == null) {
            return ResponseEntity.notFound().build();
        }
        Seller updatedSeller = sellerService.updateSeller(id, seller);
        return ResponseEntity.ok(updatedSeller);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSeller(@PathVariable String id) {
        Seller existingSeller = sellerService.getSeller(id);
        if (existingSeller == null) {
            return ResponseEntity.notFound().build();
        }
        sellerService.deleteSeller(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Seller> deactivateSeller(@PathVariable String id) {
        Seller seller = sellerService.deactivateSeller(id);
        return seller != null ? ResponseEntity.ok(seller) : ResponseEntity.notFound().build();
    }
} 