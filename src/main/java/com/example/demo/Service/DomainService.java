package com.example.demo.Service;

import com.example.demo.Exceptions.NotFoundEx.DomainNFException;
import com.example.demo.Exceptions.NotFoundEx.UserNFException;
import com.example.demo.models.repository.DomainRepository;
import com.example.demo.models.entities.Domain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DomainService {
    
    private DomainRepository domainRepository;

    @Autowired
    public DomainService(DomainRepository domainRepository) { this.domainRepository = domainRepository; }
    
    public Iterable<Domain> getDomains() { return domainRepository.findAll(); }
    
    public Domain getDomain(Long domainId) {
        try {
            return domainRepository.findDomainById(domainId).orElseThrow(()->
                    new DomainNFException("Domain with id: " + domainId + "Not Found"));
        }catch (Exception e){
            throw new UserNFException("Failed translation DB-domain to Model-domain", e);
        }
    }
}
