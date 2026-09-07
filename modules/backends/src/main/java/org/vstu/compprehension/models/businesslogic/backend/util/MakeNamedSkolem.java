package org.vstu.compprehension.models.businesslogic.backend.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.reasoner.rulesys.RuleContext;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;
import org.apache.jena.shared.JenaException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Bind a URI node to the first argument.
 * The new node will be named after the URI (normally a class) provided in the second argument.
 * For any given combination of the remaining arguments
 * the same URI node will be returned.
 */
public class MakeNamedSkolem extends BaseBuiltin {

    /**
     * Return a name for this builtin, normally this will be the name of the
     * functor that will be used to invoke it.
     */
    @Override
    public String getName() {
        return "makeNamedSkolem";
    }

    /**
     * This method is invoked when the builtin is called in a rule body.
     * @param args the array of argument values for the builtin, this is an array
     * of Nodes, some of which may be Node_RuleVariables.
     * @param length the length of the argument list, may be less than the length of the args array
     * for some rule engines
     * @param context an execution context giving access to other relevant data
     * @return return true if the buildin predicate is deemed to have succeeded in
     * the current environment
     */
    @Override
    public boolean bodyCall(Node[] args, int length, RuleContext context) {
        Node targetVar = args[0];
        Node targetClass = args[1];  // URI prefix
        StringBuilder key = new StringBuilder();
        for (int i = 2; i < length; i++) {
            Node n = getArg(i, args, context);
            if (n.isBlank()) {
                key.append("B"); key.append(n.getBlankNodeLabel());
            } else if (n.isURI()) {
                key.append("U"); key.append(n.getURI());
            } else if (n.isLiteral()) {
                key.append("L"); key.append(n.getLiteralLexicalForm());
                if (n.getLiteralLanguage() != null) key.append("@" + n.getLiteralLanguage());
                if (n.getLiteralDatatypeURI() != null) key.append("^^" + n.getLiteralDatatypeURI());
            } else {
                key.append("O"); key.append(n);
            }
        }

        try {
            MessageDigest digester = MessageDigest.getInstance("MD5");
            digester.reset();
            byte[] digest = digester.digest(key.toString().getBytes());
            String label = Base64.encodeBase64URLSafeString(digest);
            // join hash with target URI
            label = targetClass.getURI() + "_" + label;
            Node skolem = NodeFactory.createURI(label);
            // // add rdf:type relation with targetClass  -- ??
            // context.add(new Triple(skolem, RDF.type.asNode(), targetClass));
            return context.getEnv().bind(targetVar, skolem);
        } catch (NoSuchAlgorithmException e) {
            throw new JenaException(e);
        }
    }

}
