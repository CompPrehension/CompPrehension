package com.example.demo.Service;

import com.example.demo.Exceptions.NotFoundEx.DomainNFException;
import com.example.demo.Exceptions.NotFoundEx.UserNFException;
import com.example.demo.models.Dao.DomainDao;
import com.example.demo.models.entities.Domain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class DomainService {
    
    private DomainDao domainDao;

    @Autowired
    public DomainService(DomainDao domainDao) { this.domainDao = domainDao; }
    
    public Iterable<Domain> getDomains() { return domainDao.findAll(); }
    
    public Domain getDomain(Long domainId) {
        try {
            return domainDao.findDomainById(domainId).orElseThrow(()->
                    new DomainNFException("Domain with id: " + domainId + "Not Found"));
        }catch (Exception e){
            throw new UserNFException("Failed translation DB-domain to Model-domain", e);
        }
    }
}
