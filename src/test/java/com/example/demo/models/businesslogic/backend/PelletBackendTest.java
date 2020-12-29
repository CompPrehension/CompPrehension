package com.example.demo.models.businesslogic.backend;

import com.example.demo.models.businesslogic.Law;
import com.example.demo.models.businesslogic.PositiveLaw;
import com.example.demo.models.businesslogic.domains.ProgrammingLanguageExpressionDomain;
import com.example.demo.models.entities.BackendFact;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Relation {
    int indexFrom;
    List<Integer> indexesTo;
}

class Term {
    Term(String text) {
        String[] parts = text.split("\\$", 2);

        Text = parts[0];
        if (parts.length > 1) {
            StudentPos = Optional.of(Integer.parseInt(parts[1]));
        } else {
            StudentPos = Optional.empty();
        }
    }

    public Optional<Integer> getStudentPos() {
        return StudentPos;
    }

    public void setStudentPos(Integer pos) {
        StudentPos = Optional.of(pos);
    }

    public String getText() {
        return Text;
    }

    Optional<Integer> StudentPos;
    String Text;
}

class Expression {
    List<Term> Terms;

    public Expression(List<String> expression) {
        Terms = new ArrayList<>();
        for (String part : expression) {
            Terms.add(new Term(part));
        }
    }

    public List<String> getTokens() {
        List<String> tokens = new ArrayList<>();
        for (Term term : getTerms()) {
            tokens.add(term.Text);
        }
        return tokens;
    }

    public List<Term> getTerms() {
        return Terms;
    }

    int size() {
        return Terms.size();
    }

}

@SpringBootTest
class PelletBackendTest {
    @Autowired
    ProgrammingLanguageExpressionDomain domain;

    static Optional<Integer> getIndexFromName(String name, boolean allowNotZeroStep) {
        Assertions.assertTrue(name.startsWith("op__"), name);
        String[] parts = name.split("__");
        assertEquals(3, parts.length, name);
        if (allowNotZeroStep || parts[1].equals("0")) {
            return Optional.of(Integer.parseInt(parts[2]));
        }
        return Optional.empty();
    }

    static HashMap<Integer, Set<Integer>> getObjectPropertyRelationsByIndex(List<BackendFact> solution) {
        HashMap<Integer, Set<Integer>> result = new HashMap<>();
        for (BackendFact fact : solution) {
            Optional<Integer> from = getIndexFromName(fact.getSubject(), false);
            Optional<Integer> to = getIndexFromName(fact.getObject(), true);
            if (from.isPresent() && to.isPresent()) {
                if (!result.containsKey(from.get())) {
                    result.put(from.get(), new HashSet<>());
                }
                result.get(from.get()).add(to.get());
            }
        }
        return result;
    }

    static public HashMap<Integer, Set<Integer>> getObjectPropertyRelationsByIndexFromJson(String jsonRelations, int maxIndex) {
        HashMap<Integer, Set<Integer>> result = new HashMap<>();

        Relation[] relations = new Gson().fromJson(
                jsonRelations,
                Relation[].class);

        for (Relation relation : relations) {
            if (!result.containsKey(relation.indexFrom)) {
                result.put(relation.indexFrom, new HashSet<>());
            }
            for (Integer indexTo : relation.indexesTo) {
                result.get(relation.indexFrom).add(indexTo);
            }
        }

        return result;
    }

    static public void checkObjectProperty(List<BackendFact> solution, String jsonRelations, int maxIndex) {
        HashMap<Integer, Set<Integer>> real = getObjectPropertyRelationsByIndex(solution);
        HashMap<Integer, Set<Integer>> exp = getObjectPropertyRelationsByIndexFromJson(jsonRelations, maxIndex);
        Assertions.assertEquals(exp, real);
    }

    static public Expression getExpressionFromJson(String jsonExpression) {
        return new Expression(
                new Gson().fromJson(
                        jsonExpression,
                        new TypeToken<List<String>>() {}.getType()));
    }

    public void checkObjectProperty(javax.json.JsonObject object, String objectProperty) {
        Expression expression = getExpressionFromJson(object.get("expression").toString());
        String jsonRelations = object.get("relations").toString();

        PelletBackend backend = new PelletBackend();
        Law law = new PositiveLaw(
                "test",
                domain.getAllLaws(),
                List.of(),
                List.of()
        );
        List<BackendFact> statement = domain.getBackendFacts(expression.getTokens());
        List<BackendFact> solution = backend.solve(List.of(law), statement, List.of(objectProperty));

        checkObjectProperty(solution, jsonRelations, expression.size());
    }

    public void checkObjectProperty(javax.json.JsonObject object) {
        String objectProperty = object.getString("objectProperty");
        checkObjectProperty(object, objectProperty);
    }

//    @ParameterizedTest
//    @JsonFileSource(resources = "../../../../../../before-test-data.json")
//    public void BeforeTest(javax.json.JsonObject object) {
//        checkObjectProperty(object, "before");
//    }
//
//    @ParameterizedTest
//    @JsonFileSource(resources = "../../../../../../has-operand-test-data.json")
//    public void HasOperandTest(javax.json.JsonObject object) {
//        checkObjectProperty(object, "ast_edge");
//    }
//
//    @ParameterizedTest
//    @JsonFileSource(resources = "../../../../../../simple-ontology-test-data.json")
//    public void SimpleOntologyTest(javax.json.JsonObject object) {
//        checkObjectProperty(object);
//    }
}