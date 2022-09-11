package org.vstu.compprehension.models.businesslogic;
import org.vstu.compprehension.Service.DomainService;
import org.vstu.compprehension.models.businesslogic.backend.Backend;
import org.vstu.compprehension.models.businesslogic.backend.PelletBackend;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.NoSuchElementException;

public class Core {
    
    @Autowired
    private DomainService domainService;
    
    //TODO: Не работает, уточнить
    private Map<String, Domain> domainMap/* = new HashMap<Long, Domain>() {
        {
            put((long)0, new TestDomain(domainService.getDomain((long)0)));
        }
    }*/;

    @Autowired
    private PelletBackend pelletBackend;
    
    public Backend getDefaultBackend() {
        return pelletBackend;
    }
    
    public Domain getDomain(String domainId) {
        
        if (!domainMap.containsKey(domainId)) {
            throw new NoSuchElementException("Domain with id: " + domainId + "Not Found");
        }
        
        return domainMap.get(domainId);
    }
}
