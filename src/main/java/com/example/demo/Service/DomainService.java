package com.example.demo.Service;

import com.example.demo.Exceptions.NotFoundEx.DomainNFException;
import com.example.demo.Exceptions.NotFoundEx.UserNFException;
import com.example.demo.models.businesslogic.Domain;
import com.example.demo.models.businesslogic.ProgrammingLanguageExpressionDomain;
import com.example.demo.models.repository.DomainRepository;
import com.example.demo.models.entities.DomainEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DomainService {
    
    private DomainRepository domainRepository;

    @Autowired
    public DomainService(DomainRepository domainRepository) { this.domainRepository = domainRepository; }
    
    public Iterable<DomainEntity> getDomainEntities() { return domainRepository.findAll(); }

    public DomainEntity createDomainEntity(String domainName, String version) {
        DomainEntity domainEntity = new DomainEntity();
        domainEntity.setName(domainName);
        domainEntity.setVersion(version);
        domainRepository.save(domainEntity);
        return domainEntity;
    }

    public boolean hasDomainEntity(String domainName) {
        return domainRepository.existsById(domainName);
    }

    public DomainEntity getDomainEntity(String domainName) {
        try {
            return domainRepository.findById(domainName).orElseThrow(()->
                    new DomainNFException("Domain with id: " + domainName + "Not Found"));
        }catch (Exception e){
            throw new UserNFException("Failed translation DB-domain to Model-domain", e);
        }
    }

    @Autowired
    private ProgrammingLanguageExpressionDomain programmingLanguageExpressionDomain;

    public Domain getDomain(String name) throws Exception {
        if (name.equals(ProgrammingLanguageExpressionDomain.name)) {
            return programmingLanguageExpressionDomain;
        } else {
            throw new Exception();
        }
    }
}
