package org.vstu.compprehension.businesslogic.domains;

import its.model.definition.MetadataPropertyValue;
import its.model.nodes.DecisionTreeElement;
import org.junit.jupiter.api.Test;
import org.vstu.compprehension.businesslogic.Concept;
import org.vstu.compprehension.businesslogic.Law;
import org.vstu.compprehension.businesslogic.Skill;
import org.vstu.compprehension.enums.Language;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class DecisionTreeDomainLocalizationContract {

    protected static final List<String> LOCALES = List.of("RU", "EN");
    protected static final List<Language> LANGUAGES = List.of(Language.RUSSIAN, Language.ENGLISH);
    private static final Pattern CYRILLIC = Pattern.compile("[А-Яа-яЁё]");
    private static final Pattern GRAMMATICAL_CASE = Pattern.compile("\\[case='[^']*']");
    private static final Pattern TEMPLATE = Pattern.compile("\\$\\{[^{}]*(\\{[^{}]*}[^{}]*)*}");

    protected abstract DecisionTreeReasoningDomain domain();

    protected abstract List<String> messageBundles();

    protected boolean isLanguageSpecificMessageKey(String key) {
        return false;
    }

    /** Каждый локализованный атрибут дерева решений есть на обоих языках. */
    @Test
    protected void decisionTreeTextsExistInEveryLocale() {
        // Arrange.
        var elements = decisionTreeElements();

        // Act.
        var gaps = new ArrayList<String>();
        for (var element : elements) {
            var byLocale = localizedProperties(element);
            var all = new HashSet<String>();
            byLocale.values().forEach(all::addAll);
            for (var locale : LOCALES) {
                for (var property : all) {
                    if (!byLocale.getOrDefault(locale, Set.of()).contains(property)) {
                        gaps.add(locale + " " + property + " at " + describe(element));
                    }
                }
            }
        }

        // Assert.
        assertFalse(elements.isEmpty());
        assertEquals(List.of(), gaps);
    }

    /** Английские тексты дерева написаны не по-русски. */
    @Test
    protected void englishDecisionTreeTextsAreNotRussian() {
        // Arrange.
        var elements = decisionTreeElements();

        // Act.
        var russianInEnglish = new ArrayList<String>();
        for (var element : elements) {
            for (var entry : element.getMetadata().getEntries()) {
                if ("EN".equals(entry.getLocCode()) && CYRILLIC.matcher(withoutTemplates(String.valueOf(entry.getValue()))).find()) {
                    russianInEnglish.add(entry.getPropertyName() + " at " + describe(element));
                }
            }
        }

        // Assert.
        assertEquals(List.of(), russianInEnglish);
    }

    /** Файлы сообщений домена содержат одинаковые ключи на обоих языках. */
    @Test
    protected void messageBundlesHaveSameKeysInEveryLanguage() {
        for (var bundle : messageBundles()) {
            // Arrange.
            var russian = bundleKeys(bundle, "ru");
            var english = bundleKeys(bundle, "en");
            russian.removeIf(this::isLanguageSpecificMessageKey);
            english.removeIf(this::isLanguageSpecificMessageKey);

            // Act.
            var onlyRussian = new HashSet<>(russian);
            onlyRussian.removeAll(english);
            var onlyEnglish = new HashSet<>(english);
            onlyEnglish.removeAll(russian);

            // Assert.
            assertFalse(russian.isEmpty(), bundle);
            assertEquals(Set.of(), onlyRussian, bundle + ": только по-русски");
            assertEquals(Set.of(), onlyEnglish, bundle + ": только по-английски");
        }
    }

    /** Название и описание домена локализованы. */
    @Test
    protected void domainNameAndDescriptionAreLocalized() {
        for (var language : LANGUAGES) {
            // Act.
            var displayName = domain().getDisplayName(language);
            var description = domain().getDescription(language);

            // Assert.
            assertFalse(displayName.isBlank());
            assertFalse(displayName.contains("display_name"));
            assertTrue(description != null && !description.isBlank() && !description.contains("description"));
        }
        assertNotEquals(domain().getDisplayName(Language.RUSSIAN), domain().getDisplayName(Language.ENGLISH));
    }

    /** У концептов, видимых преподавателю, есть подписи на обоих языках. */
    @Test
    protected void teacherVisibleConceptsHaveDisplayNames() {
        // Arrange.
        var names = new ArrayList<String>();
        domain().getConceptsSimplifiedHierarchy(Concept.FLAG_VISIBLE_TO_TEACHER).forEach((parent, children) -> {
            names.add(parent.getName());
            children.forEach(child -> names.add(child.getName()));
        });

        // Act & Assert.
        assertFalse(names.isEmpty());
        assertEquals(List.of(), missingDisplayNames(names, "concept.", domain()::getConceptDisplayName));
    }

    /** У законов, видимых преподавателю, есть подписи на обоих языках. */
    @Test
    protected void teacherVisibleLawsHaveDisplayNames() {
        // Arrange.
        var names = new ArrayList<String>();
        domain().getLawsSimplifiedHierarchy(Law.FLAG_VISIBLE_TO_TEACHER).forEach((parent, children) -> {
            names.add(parent.getName());
            children.forEach(child -> names.add(child.getName()));
        });

        // Act & Assert.
        assertEquals(List.of(), missingDisplayNames(names, "law.", domain()::getLawDisplayName));
    }

    /** У умений, видимых преподавателю, есть подписи на обоих языках. */
    @Test
    protected void teacherVisibleSkillsHaveDisplayNames() {
        // Arrange.
        var parents = new ArrayList<String>();
        var children = new ArrayList<String>();
        domain().getSkillSimplifiedHierarchy(Skill.FLAG_VISIBLE_TO_TEACHER).forEach((parent, nested) -> {
            parents.add(parent.getName());
            nested.forEach(child -> children.add(child.getName()));
        });

        // Act & Assert.
        assertFalse(parents.isEmpty());
        assertEquals(List.of(), missingDisplayNames(parents, "skill.", domain()::getSkillDisplayName));
        assertEquals(List.of(), missingDisplayNames(children, "law.", domain()::getLawDisplayName));
    }

    private interface DisplayName {
        String of(String name, Language language);
    }

    private static List<String> missingDisplayNames(List<String> names, String prefix, DisplayName displayName) {
        var missing = new ArrayList<String>();
        for (var name : names) {
            for (var language : LANGUAGES) {
                var shown = displayName.of(name, language);
                if (shown.isBlank() || shown.equals(prefix + name) || shown.endsWith("." + name)) {
                    missing.add(language + " " + prefix + name);
                }
            }
        }
        return missing;
    }

    protected List<DecisionTreeElement> decisionTreeElements() {
        var elements = new ArrayList<DecisionTreeElement>();
        for (var model : domain().getDomainSolvingModels()) {
            for (var tree : model.getDecisionTrees().values()) {
                collect(tree, elements, new HashSet<>());
            }
        }
        return elements;
    }

    private static void collect(DecisionTreeElement element, List<DecisionTreeElement> into, Set<DecisionTreeElement> seen) {
        if (!seen.add(element)) {
            return;
        }
        into.add(element);
        for (var linked : element.getLinkedElements()) {
            collect(linked, into, seen);
        }
    }

    protected static Map<String, Set<String>> localizedProperties(DecisionTreeElement element) {
        var byLocale = new HashMap<String, Set<String>>();
        for (MetadataPropertyValue entry : element.getMetadata().getEntries()) {
            if (entry.getLocCode() != null) {
                byLocale.computeIfAbsent(entry.getLocCode(), k -> new HashSet<>()).add(entry.getPropertyName());
            }
        }
        return byLocale;
    }

    protected static String describe(DecisionTreeElement element) {
        var metadata = element.getMetadata();
        var alias = metadata.containsUnlocalized("alias") ? metadata.get("alias") : null;
        var id = metadata.containsUnlocalized("TEMPLATING_ID") ? metadata.get("TEMPLATING_ID") : null;
        var text = metadata.getEntries().stream()
                .filter(entry -> entry.getLocCode() != null)
                .map(entry -> String.valueOf(entry.getValue()))
                .findFirst()
                .map(value -> value.length() > 60 ? value.substring(0, 60) + "…" : value)
                .orElse(null);
        return element.getClass().getSimpleName() + "(id=" + id + ", alias=" + alias + ", text=" + text + ")";
    }

    private static String withoutTemplates(String text) {
        return TEMPLATE.matcher(GRAMMATICAL_CASE.matcher(text).replaceAll("")).replaceAll("");
    }

    private static Set<String> bundleKeys(String bundle, String language) {
        var resource = bundle + "_" + language + ".properties";
        try (var stream = DecisionTreeDomainLocalizationContract.class.getClassLoader().getResourceAsStream(resource)) {
            var properties = new Properties();
            properties.load(new InputStreamReader(Objects.requireNonNull(stream, resource), StandardCharsets.UTF_8));
            return new HashSet<>(properties.stringPropertyNames());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
