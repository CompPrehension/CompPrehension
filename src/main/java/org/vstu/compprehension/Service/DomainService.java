package org.vstu.compprehension.Service;

import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDomain;
import org.vstu.compprehension.models.repository.DomainRepository;
import org.vstu.compprehension.models.entities.DomainEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

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
        return domainRepository.findById(domainName).orElseThrow(() ->
                    new NoSuchElementException("Domain with id: " + domainName + " not Found"));
    }

    @Autowired
    private ProgrammingLanguageExpressionDomain programmingLanguageExpressionDomain;

    /*public Domain getDomain(String name) throws Exception {
        if (name.equals(ProgrammingLanguageExpressionDomain.getName())) {
            return programmingLanguageExpressionDomain;
        } else {
            throw new Exception();
        }
    }*/
}
