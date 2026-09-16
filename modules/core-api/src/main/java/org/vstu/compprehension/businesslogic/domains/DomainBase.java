package org.vstu.compprehension.businesslogic.domains;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.data.question.GeneratedQuestionData;
import org.vstu.compprehension.data.question.QuestionContentData;
import org.vstu.compprehension.data.question.QuestionMetadataData;
import org.vstu.compprehension.services.RandomProvider;
import org.vstu.compprehension.businesslogic.*;
import org.vstu.compprehension.enums.Language;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.stream.Stream;

@Log4j2
public abstract class DomainBase implements Domain {
    public static final String NAME_PREFIX_IS_HUMAN = "[human]";
    @Getter
    private final DomainStructure structure;
    @Getter
    protected final RandomProvider randomProvider;
    private final String domainId;

    protected DomainBase(String domainId, RandomProvider randomProvider, DomainStructure structure) {
        this.domainId = domainId;
        this.randomProvider = randomProvider;
        this.structure = structure;
    }

    public @NotNull String getDomainId() {
        return domainId;
    }

    public @Nullable Tag getTag(@NotNull String name) {
        return getTags().get(name);
    }

    public @NotNull List<Tag> resolveTags(@NotNull Collection<String> tagNames) {
        return tagNames.stream()
                .map(this::getTag)
                .filter(Objects::nonNull)
                .toList();
    }

    public @NotNull String getQuestionUniqueTemplateName(@NotNull QuestionContentData question) {
        return getDomainId() + Optional.ofNullable(question.getMetadata())
                .map(QuestionMetadataData::getTemplateId)
                .filter(Objects::nonNull)
                .map(templateId -> ":template-id:" + templateId)
                .orElse(":question:" + question.getQuestionName());
    }
    public abstract @NotNull Map<String, Tag> getTags();

    public Collection<PositiveLaw> getPositiveLaws() {
        return structure.laws().positive().values();
    }
    public Collection<NegativeLaw> getNegativeLaws() {
        return structure.laws().negative().values();
    }

    public Collection<Concept> getConcepts() {
        return structure.concepts().values();
    }
    public String getConceptDisplayName(String conceptName, Language language) {
        return getMessage(conceptName, "concept.", language);
    }

    public String getLawDisplayName(String lawName, Language language) {
        return getMessage(lawName, "law.", language);
    }

    public String getSkillDisplayName(String skillName, Language language) {
        return getMessage(skillName, "skill.", language);
    }

    public @Nullable PositiveLaw getPositiveLaw(String name) {
        return structure.laws().positive().get(name);
    }

    public @Nullable NegativeLaw getNegativeLaw(String name) {
        return structure.laws().negative().get(name);
    }

    public @Nullable Law getLaw(String name) {
        var negative = getNegativeLaw(name);
        if (negative != null)
            return negative;
        return getPositiveLaw(name);
    }

    public Collection<? extends Law> getLawWithChildren(String name_) {
        return getLawsWithChildren(List.of(name_));
    }

    public Collection<? extends Law> getLawsWithChildren(Collection<String> names) {
        Set<String> res = new HashSet<>();
        Set<String> pool = new HashSet<>(names);
        while (!pool.isEmpty()) {
            // copy concepts from pool to res
            res.addAll(pool);

            for (String name : new HashSet<>(pool)) {
                pool.remove(name);
                Law currLaw = getLaw(name);
                if (currLaw != null) {
                    // try to add all children of current concept
                    pool.addAll(currLaw.getChildLaws().stream().map(Law::getName).collect(Collectors.toSet()));
                    pool.removeAll(res);  // guard: don't allow infinite recursion.
                }
            }
        }
        return res.stream().map(this::getLaw).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public List<PositiveLaw> getPositiveLawWithImplied(String name) {
        PositiveLaw law = getPositiveLaw(name);
        if (law == null)
            return List.of();

        List<PositiveLaw> res = new ArrayList<>(List.of(law));
        List<String> impliedNames = law.getImpliesLaws();
        if (impliedNames == null)
            return res;

        for (String lawName : impliedNames) {
            // danger: infinite recursion is possible
            res.addAll(getPositiveLawWithImplied(lawName));
        }
        return res;
    }
    public List<NegativeLaw> getNegativeLawWithImplied(String name) {
        if (name == null)
            return List.of();
        NegativeLaw law = getNegativeLaw(name);
        if (law == null)
            return List.of();

        List<NegativeLaw> res = new ArrayList<>(List.of(law));
        List<String> impliedNames = law.getImpliesLaws();
        if (impliedNames == null)
            return res;

        for (String lawName : impliedNames) {
            // danger: infinite recursion is possible
            res.addAll(getNegativeLawWithImplied(lawName));
        }
        return res;
    }

    public Concept getConcept(String name) {
        return structure.concepts().get(name);
    }

    public List<Concept> getAllConcepts() {
        return new ArrayList<>(structure.concepts().values());
    }

    public List<Skill> getAllSkills() {
        return new ArrayList<>(structure.skills().values());
    }

    public List<Law> getAllLaws() {
        ArrayList<Law> result = new ArrayList<>(structure.laws().positive().values());
        result.addAll(structure.laws().negative().values());
        return result;
    }

    @NotNull
    public List<Tag> getAllTags() {
        return new ArrayList<>(getTags().values());
    }

    private static List<Long> splitIntoBits(long value) {
        List<Long> result = new ArrayList<>();
        long mask = 1L;

        for (int i = 0; i < Long.SIZE; i++) {
            if ((value & mask) != 0) {
                result.add(mask);
            }
            mask <<= 1;
        }
        return result;
    }

    public List<Skill> skillsFromBitmask(long bitmask) {
        List<Long> masks = splitIntoBits(bitmask);
        List<Skill> result = new ArrayList<>();
        for (long mask : masks) {
            for (Skill skill : getAllSkills()) {
                if (skill.getBitmask() == mask) {
                    result.add(skill);
                }
            }
        }
        return result;
    }

    public List<Concept> conceptsFromBitmask(long bitmask) {
        List<Long> masks = splitIntoBits(bitmask);
        List<Concept> result = new ArrayList<>();
        for (long mask : masks) {
            for (Concept concept : getAllConcepts()) {
                if (concept.getBitmask() == mask) {
                    result.add(concept);
                }
            }
        }
        return result;
    }

    public List<NegativeLaw> negativeLawFromBitmask(long bitmask) {
        return lawsFromBitmask(structure.laws().negative().values(), bitmask);
    }

    public List<PositiveLaw> positiveLawFromBitmask(long bitmask) {
        return lawsFromBitmask(structure.laws().positive().values(), bitmask);
    }

    private static <T extends Law> List<T> lawsFromBitmask(Collection<T> laws, long bitmask) {
        List<Long> masks = splitIntoBits(bitmask);
        List<T> result = new ArrayList<>();
        for (long mask : masks) {
            for (T law : laws) {
                if (law.getBitmask() == mask) {
                    result.add(law);
                }
            }
        }
        return result;
    }

    public List<Tag> tagsFromBitmask(long bitmask) {
        List<Long> masks = splitIntoBits(bitmask);
        List<Tag> result = new ArrayList<>();
        for (long mask : masks) {
            for (Tag tag : getAllTags()) {
                if (tag.getBitmask() == mask) {
                    result.add(tag);
                }
            }
        }
        return result;
    }

    public Skill getSkill(String name) {
        return structure.skills().get(name);
    }

    /** Get skills organized into one-level hierarchy
     * @return map representing groups of skills (base skill -> skills in the group)
     */
    public Map<Skill, List<Skill>> getSkillSimplifiedHierarchy(DomainItemFlag... requiredFlags) {
        Map<Skill, List<Skill>> res = new TreeMap<>();
        for (Skill skill : getAllSkills()) {
            if (skill.hasFlags(requiredFlags)) {
                res.put(skill, new ArrayList<>());
            }
        }
        return res;
    }

    /** Get concepts with all given flags (e.g. visible) organized into two-level hierarchy
     * @return map representing groups of concepts (base concept -> concepts in the group)
     */
    public Map<Concept, List<Concept>> getConceptsSimplifiedHierarchy(DomainItemFlag... requiredFlags) {
        Map<Concept, List<Concept>> res = new TreeMap<>();
        Set<Concept> wanted = structure.concepts().values().stream().filter(t -> t.hasFlags(requiredFlags)).collect(Collectors.toSet());
        Set<Concept> added = new HashSet<>();
        for (Concept ct : new ArrayList<>(wanted)) {
            // ensure we are dealing with bottom-level concept
            Collection<Concept> children = this.getConceptWithChildren(ct.getName());
            children.remove(ct);
            boolean hasChildren =
                    children.stream().anyMatch(wanted::contains);
            if (hasChildren) {
                continue;  // skip non-bottom concepts
            }

            Concept nearestWantedBase = null;
            List<Concept> bases = new ArrayList<>(ct.getBaseConcepts());
            while (!bases.isEmpty()) {
                for (Concept base : new ArrayList<>(bases)) {
                    if (wanted.contains(base)) {
                        nearestWantedBase = base;
                        bases.clear();
                        break;
                    }
                    bases.remove(base);
                    bases.addAll(base.getBaseConcepts());
                }
            }
            Concept key;
            List<Concept> value;

            if (nearestWantedBase != null) {
                // concept is within a group
                key = nearestWantedBase;
                value = new ArrayList<>(List.of(ct));
                added.add(ct);

            } else {
                // concept does not belong to any group (has no bases we want)
                key = ct;
                value = new ArrayList<>();
            }
            // put into a group or as top-level
            if (res.containsKey(key)) {
                List<Concept> arr = res.get(key);
                for (Concept oneValue : value)
                    if (!arr.contains(oneValue)) {
                        arr.add(oneValue);
                        added.add(oneValue);
                    }
            } else {
                res.put(key, value);
                added.add(key);
            }
        }
        // add all top-level bases we skipped
        wanted.removeAll(added);
        for (Concept t : wanted) {
            res.put(t, new ArrayList<>());
        }

        // sort list items
        for (var list : res.values()) {
            list.sort(TreeNodeWithBitmask::compareTo);
        }

        return res;
    }

    /** Get laws with all given flags (e.g. visible) organized into two-level hierarchy
     * @return map representing groups of laws (base law -> laws in the group)
     */
    public Map<Law, List<Law>> getLawsSimplifiedHierarchy(DomainItemFlag... requiredFlags) {
        Map<Law, List<Law>> res = new TreeMap<>();
        Set<Law> wanted = Stream.concat(this.getPositiveLaws().stream(), this.getNegativeLaws().stream())
                .filter(t -> t.hasFlags(requiredFlags)).collect(Collectors.toSet());
        Set<Law> added = new HashSet<>();
        for (Law ct : new ArrayList<>(wanted)) {
            // ensure we are dealing with bottom-level law
            var children = this.getLawWithChildren(ct.getName());
            children.remove(ct);
            boolean hasChildren =
                    children.stream().anyMatch(wanted::contains);
            if (hasChildren) {
                continue;  // skip non-bottom laws
            }

            Law nearestWantedBase = null;
            List<Law> bases = new ArrayList<>(ct.getLawsImplied());
            while (!bases.isEmpty()) {
                for (Law base : new ArrayList<>(bases)) {
                    if (wanted.contains(base)) {
                        nearestWantedBase = base;
                        bases.clear();
                        break;
                    }
                    bases.remove(base);
                    bases.addAll(base.getLawsImplied());
                }
            }
            Law key;
            List<Law> value;

            if (nearestWantedBase != null) {
                // law is within a group
                key = nearestWantedBase;
                value = new ArrayList<>(List.of(ct));
                added.add(ct);

            } else {
                // law does not belong to any group (has no bases we want)
                key = ct;
                value = new ArrayList<>();
            }
            // put into a group or as top-level
            if (res.containsKey(key)) {
                List<Law> arr = res.get(key);
                for (Law oneValue : value)
                    if (!arr.contains(oneValue)) {
                        arr.add(oneValue);
                        added.add(oneValue);
                    }
            } else {
                res.put(key, value);
                added.add(key);
            }
        }
        // add all top-level bases we skipped
        wanted.removeAll(added);
        for (Law t : wanted) {
            res.put(t, new ArrayList<>());
        }

        // sort list items
        for (var list : res.values()) {
            list.sort(TreeNodeWithBitmask::compareTo);
        }

        return res;
    }

    public Collection<Concept> getChildrenOfConcept(String name_) {
        return getConceptsWithChildren(List.of(name_)).stream()
                .filter(t -> !name_.equals(t.getName()))
                .collect(Collectors.toList());
    }

    public Collection<Concept> getConceptWithChildren(String name_) {
        return getConceptsWithChildren(List.of(name_));
    }

    public Collection<Concept> getConceptsWithChildren(Collection<String> names) {
        Set<String> res = new HashSet<>();
        Set<String> pool = new HashSet<>(names);
        while (!pool.isEmpty()) {
            // copy concepts from pool to res
            res.addAll(pool);  // .stream().map(this::getConcept).collect(Collectors.toSet()));
//            res.addAll(pool.stream().flatMap(n -> tm.get(n).stream()).collect(Collectors.toSet()));
            for (String name : new HashSet<>(pool)) {
                pool.remove(name);
                Concept currConcept = getConcept(name);
                if (currConcept != null) {
                    // try to add all children of current concept
                    pool.addAll(currConcept.getChildConcepts().stream().map(Concept::getName).collect(Collectors.toSet()));
                    pool.removeAll(res);  // guard: don't allow infinite recursion.
                }
            }
        }
        return res.stream().map(this::getConcept).filter(Objects::nonNull).collect(Collectors.toSet());
//        return new ArrayList<>(res);
    }


    /**
     * Get localized domain-specific string
     * @param messageKey language string key
     * @param preferredLanguage target language (fallback is english)
     * @return localized message
     */
    public abstract String getMessage(String messageKey, Language preferredLanguage);

    /**
     * Get localized domain-specific string
     * @param messageKey language string key
     * @param prefix prefix for language key (kind of namespace)
     * @param preferredLanguage target language (fallback is english)
     * @return localized message
     */
    public String getMessage(String messageKey, String prefix, Language preferredLanguage) {
        return getMessage(prefix + messageKey, preferredLanguage);
    }

    public String getDefaultQuestionType() {
        return getDefaultQuestionType(false);
    }

    public String getDefaultQuestionType(boolean supplementary) {
        return null;  // the default
    }

    public List<Tag> getDefaultQuestionTags(String questionDomainType) {
        // the default
        return new ArrayList<>();
    }

    public QuestionRequest ensureQuestionRequestValid(QuestionRequest questionRequest) {
        return questionRequest;
    }

    public List<Law> getQuestionLaws(String questionDomainType, List<Tag> tags) {
        Collection<PositiveLaw> positiveLaws = getQuestionPositiveLaws(questionDomainType, tags);
        Collection<NegativeLaw> negativeLaws = getQuestionNegativeLaws(questionDomainType, tags);
        List<Law> laws = new ArrayList<>();
        laws.addAll(positiveLaws);
        laws.addAll(negativeLaws);
        return laws;
    }

    /** Check if a law needed for a question with tags specified.
     * Returns true in two cases:
     * 1) the law has no tags attached, or
     * 2) the two sets of tag names do intersect (i.e. contain at least one common tag).
     * @param law Law to check tags for
     * @param tags Tags to check against.
     * @return true if any common tag exists.
     */
    public static boolean isLawNeededByQuestionTags(Law law, Collection<Tag> tags) {
        boolean needLaw = true;
        for (Tag tag : law.getTags()) {  // law having no tags is still needed.
            boolean inQuestionTags = false;
            for (Tag questionTag : tags) {
                if (questionTag.getName().equals(tag.getName())) {
                    inQuestionTags = true;
                    break;
                }
            }
            if (!inQuestionTags) {
                needLaw = false;
                break;
            }
        }
        return needLaw;
    }

    /**
     * Return all question templates
     */
    protected abstract List<GeneratedQuestionData> getQuestionTemplates();

    /**
     * Find a question template in in-memory suite of Domain's `questions`
     * @param tags question tags
     * @param targetConcepts concepts that should be in question
     * @param deniedConcepts concepts that should not be in question
     * @param targetNegativeLaws negative laws that should be in question
     * @param deniedNegativeLaws negative laws that should not be in question
     * @param forbiddenQuestions texts of question that not suit TODO: use ExerciseAttemptEntity
     * @return new question template
     */
    public GeneratedQuestionData findQuestion(List<Tag> tags, Set<String> targetConcepts, Set<String> deniedConcepts, Set<String> targetNegativeLaws, Set<String> deniedNegativeLaws, Set<String> forbiddenQuestions) {
        List<GeneratedQuestionData> questions = new ArrayList<>();

        int maxSuitCount = 0;
        int minAdditionalCount = 10000;
        for (GeneratedQuestionData q : getQuestionTemplates()) {
            var content = q.getContent();
            int targetConceptCount = 0;
            int anotherConcepts = 0;
            boolean suit = true;
            if (forbiddenQuestions.contains(content.getQuestionName()) || forbiddenQuestions.contains(NAME_PREFIX_IS_HUMAN + content.getQuestionName())) {
                continue;
            }
            for (Tag tag : tags) {
                if (!content.getTags().contains(tag.getName())) {
                    suit = false;
                    break;
                }
            }
            if (!suit) continue;
            for (String concept : q.getConcepts()) {
                if (deniedConcepts.contains(concept)) {
                    suit = false;
                    break;
                } else if (targetConcepts.contains(concept)) {
                    targetConceptCount++;
                } else {
                    anotherConcepts++;
                }
            }
            if (!suit) continue;
            for (String negativeLaw : q.getNegativeLaws()) {
                if (deniedNegativeLaws.contains(negativeLaw)) {
                    suit = false;
                    break;
                } else if (targetNegativeLaws.contains(negativeLaw)) {
                    targetConceptCount++;
                } else {
                    anotherConcepts++;
                }
            }
            if (suit) {
                if (targetConceptCount > maxSuitCount || targetConceptCount == maxSuitCount && anotherConcepts <= minAdditionalCount) {
                    if (targetConceptCount > maxSuitCount || anotherConcepts < minAdditionalCount) {
                        questions.clear();
                        maxSuitCount = targetConceptCount;
                        minAdditionalCount = anotherConcepts;
                    }
                    questions.add(q);
                }
            }
        }
        if (questions.isEmpty()) {
            return null;
        }

        for (GeneratedQuestionData question : questions) {
            log.info("Отобранный вопрос (из {}): {}", questions.size(), question.getContent().getQuestionName());
        }

        GeneratedQuestionData question = questions.get(randomProvider.getRandom().nextInt(questions.size()));
        log.info("В итоге, взят вопрос: {}", question.getContent().getQuestionName());

        // пометка в имени: этот вопрос сделан человеком
        String name = question.getContent().getQuestionName();
        if (name != null && !name.startsWith(NAME_PREFIX_IS_HUMAN)) {
            question = question.withContent(question.getContent().toBuilder()
                    .questionName(NAME_PREFIX_IS_HUMAN + name)
                    .build());
        }
        return question;
    }
}
