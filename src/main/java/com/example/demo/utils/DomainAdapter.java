package com.example.demo.utils;

import com.example.demo.models.businesslogic.domains.Domain;
import com.example.demo.models.businesslogic.domains.ProgrammingLanguageExpressionDomain;
import com.example.demo.models.businesslogic.domains.TestDomain;

import java.util.HashMap;

public class DomainAdapter {
    private static HashMap<String, Domain> domains ;
    static {
        domains = new HashMap<>();
        domains.put("TestDomain", new TestDomain());
        domains.put("ProgrammingLanguageExpressionDomain", new ProgrammingLanguageExpressionDomain());
    }

    public static Domain getDomain(String name){
        return domains.get(name);
    }
}
