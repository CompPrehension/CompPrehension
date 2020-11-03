package com.example.demo.controllers;

import com.example.demo.Exceptions.NotFoundEx.DomainNFException;
import com.example.demo.Service.DomainService;
import com.example.demo.models.entities.Domain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("domains")
public class DomainController {
    
    @Autowired
    private DomainService domainService;
    
    @GetMapping
    public ResponseEntity<Iterable<Domain>> getDomains() {
        
        try {
            Iterable<Domain> domains = domainService.getDomains();
            return new ResponseEntity<>(domains, HttpStatus.OK);
        } catch (DomainNFException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @GetMapping("{domainName}")
    public ResponseEntity<Domain> getDomain(@PathVariable String domainId) {
        
        try {
            Domain domain = domainService.getDomain(domainId);
            return new ResponseEntity<>(domain, HttpStatus.OK);
        } catch (DomainNFException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
