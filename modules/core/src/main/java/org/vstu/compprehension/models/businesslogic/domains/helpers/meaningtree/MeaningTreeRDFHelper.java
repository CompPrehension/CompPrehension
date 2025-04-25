package org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.vstu.compprehension.models.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.models.businesslogic.storage.SerializableQuestion;
import org.vstu.compprehension.models.entities.BackendFactEntity;
import org.vstu.meaningtree.MeaningTree;
import org.vstu.meaningtree.SupportedLanguage;
import org.vstu.meaningtree.exceptions.MeaningTreeException;
import org.vstu.meaningtree.nodes.Node;
import org.vstu.meaningtree.nodes.expressions.BinaryExpression;
import org.vstu.meaningtree.nodes.expressions.logical.ShortCircuitAndOp;
import org.vstu.meaningtree.nodes.expressions.logical.ShortCircuitOrOp;
import org.vstu.meaningtree.serializers.rdf.RDFDeserializer;
import org.vstu.meaningtree.utils.tokens.Token;
import org.vstu.meaningtree.utils.tokens.TokenList;
import org.vstu.meaningtree.utils.tokens.TokenType;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class MeaningTreeRDFHelper {
    public static List<BackendFactEntity> serializableToBackendFacts(List<SerializableQuestion.StatementFact> facts) {
        return facts.stream().map((SerializableQuestion.StatementFact fact) ->
                new BackendFactEntity(fact.getSubjectType(), fact.getSubject(), fact.getVerb(), fact.getObjectType(), fact.getObject())).toList();
    }

    public static List<SerializableQuestion.StatementFact> backendFactsToSerialized(List<BackendFactEntity> facts) {
        return facts.stream().map((BackendFactEntity fact) -> SerializableQuestion.StatementFact.builder()
                .verb(fact.getVerb())
                .objectType(fact.getObjectType())
                .object(fact.getObject())
                .subjectType(fact.getSubjectType())
                .subject(fact.getSubject()).build()).toList();
    }

    public static List<BackendFactEntity> factsFromModel(Model m) {
        JenaBackend jback = new JenaBackend();
        jback.createOntology("http://vstu.ru/poas/code");
        OntModel model = jback.getModel();
        model.add(m);
        List<BackendFactEntity> facts = jback.getFacts(null);
        return facts;
    }

    public static Model backendFactsToModel(List<BackendFactEntity> statementFacts) {
        JenaBackend jback = new JenaBackend();
        jback.createOntology("http://vstu.ru/poas/code");
        jback.addBackendFacts(statementFacts);
        return jback.getModel();
    }

    //TODO: improve
    static void quickFixValues(MeaningTree mt) {
        for (Node.Info info : mt) {
            if (info.node() instanceof BinaryExpression op) {
                boolean opVal = op.getAssignedValueTag() != null && (boolean) op.getAssignedValueTag();
                boolean leftVal = op.getLeft().getAssignedValueTag() != null && (boolean) op.getLeft().getAssignedValueTag();
                boolean rightVal = op.getLeft().getAssignedValueTag() != null && (boolean) op.getLeft().getAssignedValueTag();

                if (op instanceof ShortCircuitAndOp || op instanceof ShortCircuitOrOp && opVal != leftVal) {
                    op.getLeft().setAssignedValueTag(opVal);
                }
                if (op instanceof ShortCircuitAndOp && opVal) {
                    op.getRight().setAssignedValueTag(true);
                }
                if (op instanceof ShortCircuitOrOp && !opVal) {
                    op.getRight().setAssignedValueTag(false);
                }
            }
        }
    }

    public static TokenList backendFactsToTokens(List<BackendFactEntity> stmtFacts, SupportedLanguage language) {
        Model model = backendFactsToModel(stmtFacts);
        MeaningTree mt = new RDFDeserializer().deserializeTree(model);
        quickFixValues(mt);
        try {
            var result = language.createTranslator(new MeaningTreeDefaultExpressionConfig()).getTokenizer().tryTokenizeExtended(mt);
            if (result.getLeft()) {
                return result.getRight();
            } else {
                return new TokenList(List.of(new Token("Sorry, this question is unsupported. Please skip it", TokenType.UNKNOWN)));
            }
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new MeaningTreeException("Tokenizer creation failed");
        }
    }

    public static MeaningTree backendFactsToMeaningTree(List<BackendFactEntity> facts) {
        return new RDFDeserializer().deserializeTree(backendFactsToModel(facts));
    }
}
