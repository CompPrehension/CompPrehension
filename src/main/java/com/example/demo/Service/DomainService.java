package com.example.demo.Service;

import com.example.demo.Exceptions.NotFoundEx.DomainNFException;
import com.example.demo.Exceptions.NotFoundEx.UserNFException;
import com.example.demo.models.entities.Concept;
import com.example.demo.models.entities.Law;
import com.example.demo.models.repository.DomainRepository;
import com.example.demo.models.entities.Domain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DomainService {
    
    private DomainRepository domainRepository;

    @Autowired
    public DomainService(DomainRepository domainRepository) { this.domainRepository = domainRepository; }
    
    public Iterable<Domain> getDomains() { return domainRepository.findAll(); }

    public Domain getOrCreateDomain(String domainName, String version, List<Law> laws, List<Concept> concepts) {
        if (hasDomain(domainName)) {
            return getDomain(domainName);
        } else {
            return createDomain(domainName, version, laws, concepts);
        }
    }

    public Domain createDomain(String domainName, String version, List<Law> laws, List<Concept> concepts) {
        Domain domain = new Domain();
        domain.setName(domainName);
        domain.setVersion(version);
        domain.setLaws(laws);
        domain.setConcepts(concepts);
        domainRepository.save(domain);
        return domain;
    }

    public boolean hasDomain(String domainName) {
        return domainRepository.existsById(domainName);
    }

    public Domain getDomain(String domainName) {
        try {
            return domainRepository.findById(domainName).orElseThrow(()->
                    new DomainNFException("Domain with id: " + domainName + "Not Found"));
        }catch (Exception e){
            throw new UserNFException("Failed translation DB-domain to Model-domain", e);
        }
    }
}
