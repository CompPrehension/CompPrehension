package org.vstu.compprehension.Service;

import org.springframework.stereotype.Service;
import org.vstu.compprehension.models.entities.DomainEntity;
import org.vstu.compprehension.models.repository.DomainRepository;

import java.util.NoSuchElementException;

@Service
public class DomainService {
    
    private DomainRepository domainRepository;

    public DomainService(DomainRepository domainRepository) { this.domainRepository = domainRepository; }


    public DomainEntity getDomainEntity(String domainName) {
        return domainRepository.findById(domainName).orElseThrow(() ->
                    new NoSuchElementException("Domain with id: " + domainName + " not Found"));
    }
}
