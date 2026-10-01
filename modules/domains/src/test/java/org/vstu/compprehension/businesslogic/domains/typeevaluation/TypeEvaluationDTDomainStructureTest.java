package org.vstu.compprehension.businesslogic.domains.typeevaluation;

import its.model.nodes.DecisionTreeElement;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.vstu.compprehension.businesslogic.Skill;
import org.vstu.compprehension.businesslogic.domains.DecisionTreeDomainStructureContract;
import org.vstu.compprehension.businesslogic.domains.DecisionTreeReasoningDomain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TypeEvaluationDTDomainStructureTest extends DecisionTreeDomainStructureContract {

    private static final List<String> TEMPLATE_KEYS = List.of("explanation", "reason", "error_prefix", "hint_prefix");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{[^}]*}");
    private static final Pattern PYTHON_WORDS = Pattern.compile("\\b(Python|True|False|None)\\b");
    private static final Pattern LATIN = Pattern.compile("[A-Za-z]");
    private static final Pattern CODE_TAG = Pattern.compile("</?code>");

    @Override
    protected DecisionTreeReasoningDomain domain() {
        return TypeEvaluationDomainFixture.domain();
    }

    /** Реестр умений домена совпадает с умениями, которыми размечено дерево, в обе стороны. */
    @Test
    void registeredSkillsAreExactlyTreeSkills() {
        // Arrange.
        var treeSkills = new TreeSet<String>();
        for (var element : treeElements()) {
            var skill = element.getMetadata().getString("skill");
            if (skill != null) {
                treeSkills.add(skill);
            }
        }

        // Act.
        var registered = domain().getAllSkills().stream()
                .map(Skill::getName)
                .collect(Collectors.toCollection(TreeSet::new));

        // Assert.
        assertEquals(treeSkills, registered);
    }

    /**
     * Объяснения дерева не содержат кода и слов конкретного языка программирования: дерево общее для всех языков,
     * а операторы, функции и типы приходят из тега языка и условия задачи.
     */
    @Test
    void explanationsDoNotMentionProgrammingLanguage() {
        // Arrange.
        var elements = treeElements();

        // Act.
        var languageSpecific = new ArrayList<String>();
        for (var element : elements) {
            for (var language : List.of("RU", "EN")) {
                for (var key : TEMPLATE_KEYS) {
                    var template = element.getMetadata().get(language, key);
                    if (template == null) {
                        continue;
                    }
                    var text = PLACEHOLDER.matcher(template.toString()).replaceAll("");
                    if (text.contains("<code>") || PYTHON_WORDS.matcher(text).find()
                            || language.equals("RU") && LATIN.matcher(text).find()) {
                        languageSpecific.add(language + "." + key + " = " + template);
                    }
                }
            }
        }

        // Assert.
        assertEquals(List.of(), languageSpecific);
    }

    /** Названия из тега языка вставляются в HTML объяснений: знаки < и > в них экранированы, иначе браузер съест их как разметку. */
    @Test
    void tagNamesAreEscapedHtml() {
        // Arrange.
        var solvingModel = domain().getDomainSolvingModels().getFirst();

        // Act.
        var unescaped = new ArrayList<String>();
        for (var tag : domain().getTags().keySet()) {
            for (var domainClass : solvingModel.getMergedTagDomain(tag.toLowerCase()).getClasses()) {
                for (var language : List.of("RU", "EN")) {
                    var name = domainClass.getMetadata().get(language, "localizedName");
                    if (name != null && CODE_TAG.matcher(name.toString()).replaceAll("").matches(".*[<>].*")) {
                        unescaped.add(tag + " " + domainClass.getName() + ": " + name);
                    }
                }
            }
        }

        // Assert.
        assertEquals(List.of(), unescaped);
    }

    private static @NotNull Set<DecisionTreeElement> treeElements() {
        var seen = new LinkedHashSet<DecisionTreeElement>();
        var queue = new ArrayDeque<DecisionTreeElement>(TypeEvaluationTreeFixture.trees());
        while (!queue.isEmpty()) {
            var element = queue.poll();
            if (seen.add(element)) {
                queue.addAll(element.getLinkedElements());
            }
        }
        return seen;
    }
}
