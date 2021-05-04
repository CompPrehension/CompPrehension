package org.vstu.compprehension.utils;

import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDomain;
import org.vstu.compprehension.models.businesslogic.domains.ControlFlowStatementsDomain;
import org.vstu.compprehension.models.businesslogic.domains.TestDomain;

import java.util.HashMap;

public class DomainAdapter {
    private static HashMap<String, Domain> domains ;
    static {
        domains = new HashMap<>();
        domains.put("TestDomain", new TestDomain());
        domains.put("ProgrammingLanguageExpressionDomain", new ProgrammingLanguageExpressionDomain());
        domains.put("ControlFlowStatementsDomain", new ControlFlowStatementsDomain());
    }

    public static Domain getDomain(String name){
        return domains.get(name);
    }
}
