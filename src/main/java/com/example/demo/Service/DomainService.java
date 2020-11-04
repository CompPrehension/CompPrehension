package com.example.demo.Service;

import com.example.demo.Exceptions.NotFoundEx.DomainNFException;
import com.example.demo.Exceptions.NotFoundEx.UserNFException;
import com.example.demo.models.entities.Concept;
import com.example.demo.models.entities.Law;
import com.example.demo.models.repository.DomainRepository;
import com.example.demo.models.entities.DomainEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DomainService {
    
    private DomainRepository domainRepository;

    @Autowired
    public DomainService(DomainRepository domainRepository) { this.domainRepository = domainRepository; }
    
    public Iterable<DomainEntity> getDomains() { return domainRepository.findAll(); }

    public DomainEntity getOrCreateDomain(String domainName, String version, List<Law> laws, List<Concept> concepts) {
        if (hasDomain(domainName)) {
            return getDomain(domainName);
        } else {
            return createDomain(domainName, version, laws, concepts);
        }
    }

    public DomainEntity createDomain(String domainName, String version, List<Law> laws, List<Concept> concepts) {
        DomainEntity domainEntity = new DomainEntity();
        domainEntity.setName(domainName);
        domainEntity.setVersion(version);
        domainEntity.setLaws(laws);
        domainEntity.setConcepts(concepts);
        domainRepository.save(domainEntity);
        return domainEntity;
    }

    public boolean hasDomain(String domainName) {
        return domainRepository.existsById(domainName);
    }

    public DomainEntity getDomain(String domainName) {
        try {
            return domainRepository.findById(domainName).orElseThrow(()->
                    new DomainNFException("Domain with id: " + domainName + "Not Found"));
        }catch (Exception e){
            throw new UserNFException("Failed translation DB-domain to Model-domain", e);
        }
    }
}
