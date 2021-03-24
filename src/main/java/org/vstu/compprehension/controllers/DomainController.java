package org.vstu.compprehension.controllers;

import org.vstu.compprehension.Exceptions.NotFoundEx.DomainNFException;
import org.vstu.compprehension.Service.DomainService;
import org.vstu.compprehension.models.entities.DomainEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("domains")
public class DomainController {
    
    @Autowired
    private DomainService domainService;
    
    @GetMapping
    public ResponseEntity<Iterable<DomainEntity>> getDomains() {
        
        try {
            Iterable<DomainEntity> domains = domainService.getDomainEntities();
            return new ResponseEntity<>(domains, HttpStatus.OK);
        } catch (DomainNFException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @GetMapping("{domainName}")
    public ResponseEntity<DomainEntity> getDomain(@PathVariable String domainId) {
        
        try {
            DomainEntity domainEntity = domainService.getDomainEntity(domainId);
            return new ResponseEntity<>(domainEntity, HttpStatus.OK);
        } catch (DomainNFException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
