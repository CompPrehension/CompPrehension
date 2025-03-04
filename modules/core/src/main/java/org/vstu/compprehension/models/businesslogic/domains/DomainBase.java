package org.vstu.compprehension.models.businesslogic.domains;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.context.annotation.RequestScope;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.businesslogic.backend.FactBackend;
import org.vstu.compprehension.models.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.models.entities.DomainEntity;
import org.vstu.compprehension.models.entities.DomainOptionsEntity;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.utils.RandomProvider;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@RequestScope
public abstract class DomainBase implements Domain {
    public static final String NAME_PREFIX_IS_HUMAN = "[human]";
    protected Map<String, PositiveLaw> positiveLaws;
    protected Map<String, NegativeLaw> negativeLaws;
    /** name to Concept mapping */
    protected Map<String, Concept> concepts;
    protected Map<String, Skill> skills;
    @Getter
    protected String version = "";
    @Getter
    protected final RandomProvider randomProvider;
    @Getter
    private final DomainEntity domainEntity;

    public DomainBase(DomainEntity domainEntity, RandomProvider randomProvider) {
        this.domainEntity = domainEntity;
        this.randomProvider = randomProvider;
    }

    public @NotNull String getDomainId() {
        return domainEntity.getName();
    }
    @NotNull
    public String getName() {
        return domainEntity.getName();
    }
    @NotNull
    public String getShortName() {
        return domainEntity.getShortName();  // same as name by default
    }

    /**
     * A temporary method to reuse DB-stored questions between Domains
     * Is the same as {@link #getShortName()} by default
     * FIXME - replace back to getShortName()
     */
    @NotNull
    public String getShortnameForQuestionSearch(){
        return getShortName();
    }
    public DomainOptionsEntity getOptions() { return domainEntity.getOptions(); }

    public DomainEntity getEntity() {
        return domainEntity;
    }

    public @Nullable Tag getTag(@NotNull String name) {
        return getTags().get(name);
    }
    public abstract @NotNull Map<String, Tag> getTags();

    public Collection<PositiveLaw> getPositiveLaws() {
        return positiveLaws.values();
    }
    public Collection<NegativeLaw> getNegativeLaws() {
        return negativeLaws.values();
    }

    public Collection<Concept> getConcepts() {
        return concepts.values();
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
        return positiveLaws.getOrDefault(name, null);
    }

    public @Nullable NegativeLaw getNegativeLaw(String name) {
        return negativeLaws.getOrDefault(name, null);
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
                if (currLaw != null && currLaw.getChildLaws() != null) {
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
        return concepts.getOrDefault(name, null);
    }

    public List<Concept> getAllConcepts() {
        return new ArrayList<>(concepts.values());
    }

    public List<Skill> getAllSkills() {
        if (skills == null) {
            return List.of();
        }
        return new ArrayList<>(skills.values());
    }

    public List<Law> getAllLaws() {
        ArrayList<Law> result = new ArrayList<>(positiveLaws.values());
        result.addAll(negativeLaws.values());
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
            } else {
                result.add(0L);
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
        List<Long> masks = splitIntoBits(bitmask);
        List<NegativeLaw> result = new ArrayList<>();
        for (long mask : masks) {
            for (Law law : getAllLaws()) {
                if (law instanceof NegativeLaw negLaw && law.getBitmask() == mask) {
                    result.add(negLaw);
                }
            }
        }
        return result;
    }

    public List<PositiveLaw> positiveLawFromBitmask(long bitmask) {
        List<Long> masks = splitIntoBits(bitmask);
        List<PositiveLaw> result = new ArrayList<>();
        for (long mask : masks) {
            for (Law law : getAllLaws()) {
                if (law instanceof PositiveLaw posLaw && law.getBitmask() == mask) {
                    result.add(posLaw);
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
        return skills.getOrDefault(name, null);
    }

    /** Get skills organized into one-level hierarchy
     * @return map representing groups of skills (base skill -> skills in the group)
     */
    public Map<Skill, List<Skill>> getSkillSimplifiedHierarchy(int bitflags) {
        Map<Skill, List<Skill>> res = new TreeMap<>();
        for (Skill skill : getAllSkills()) {
            if (skill.getBaseSkills().isEmpty() && skill.hasFlag(bitflags)) {
                res.put(skill, new ArrayList<>());
            }
        }
        return res;
    }

    /** Get concepts with given flags (e.g. visible) organized into two-level hierarchy
     * @param requiredFlags e.g. Concept.FLAG_VISIBLE_TO_TEACHER
     * @return map representing groups of concepts (base concept -> concepts in the group)
     */
    public Map<Concept, List<Concept>> getConceptsSimplifiedHierarchy(int requiredFlags) {
        Map<Concept, List<Concept>> res = new TreeMap<>();
        Set<Concept> wanted = this.concepts.values().stream().filter(t -> t.hasFlag(requiredFlags)).collect(Collectors.toSet());
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

    /** Get laws with given flags (e.g. visible) organized into two-level hierarchy
     * @param requiredFlags e.g. Law.FLAG_VISIBLE_TO_TEACHER
     * @return map representing groups of laws (base law -> laws in the group)
     */
    public Map<Law, List<Law>> getLawsSimplifiedHierarchy(int requiredFlags) {
        Map<Law, List<Law>> res = new TreeMap<>();
        Set<Law> wanted = Stream.concat(this.getPositiveLaws().stream(), this.getNegativeLaws().stream())
                .filter(t -> t.hasFlag(requiredFlags)).collect(Collectors.toSet());
        Set<Law> added = new HashSet<>();
        for (Law ct : new ArrayList<>(wanted)) {
            // ensure we are dealing with bottom-level law
            Collection<Law> children = (Collection<Law>) this.getLawWithChildren(ct.getName());
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
                Concept currConcept =  concepts.getOrDefault(name, null);
                if (currConcept != null && currConcept.getChildConcepts() != null) {
                    // try to add all children of current concept
                    pool.addAll(currConcept.getChildConcepts().stream().map(Concept::getName).collect(Collectors.toSet()));
                    pool.removeAll(res);  // guard: don't allow infinite recursion.
                }
            }
        }
        return res.stream().map(this::getConcept).filter(Objects::nonNull).collect(Collectors.toSet());
//        return new ArrayList<>(res);
    }


    protected Concept addConcept(Concept t) {
        concepts.put(t.getName(), t);
        return t;
    }

    protected Skill addSkill(Skill t) {
        skills.put(t.getName(), t);
        return t;
    }

    protected Skill addSkill(String name) {
        return addSkill(new Skill(name));
    }

    protected Skill addSkill(String name, List<Skill> baseSkills) {
        return addSkill(new Skill(name, baseSkills));
    }

    protected Skill addSkill(String name, List<Skill> baseSkills, int bitflags) {
        return addSkill(new Skill(name, baseSkills, bitflags));
    }

    protected Concept addConcept(String name, List<Concept> bases, int flags) {
        return addConcept(new Concept(name, bases, flags));
    }

    protected void addConcepts(Collection<Concept> ts) {
        for (Concept t : ts)
            addConcept(t);
    }

    /** Set direct children to concepts. This is needed since parents (bases) of concepts are stored only */
    protected void fillConceptTree() {
        for (Concept concept : concepts.values()) {
            if (concept.getBaseConcepts() == null)
                continue;
            for (Concept base : concept.getBaseConcepts()) {
                if (base.getChildConcepts() == null) {
                    base.setChildConcepts(new HashSet<>());
                }
                base.getChildConcepts().add(concept);
            }
        }
    }

    /** Set direct children to both positive and negative Laws. This is needed since names of laws are stored only */
    protected void fillLawsTree() {
        // set direct implied (base) Laws to each Law
        for (Law t : positiveLaws.values()) {
            if (t.getImpliesLaws() == null) {
                t.setLawsImplied(List.of());
            } else {
                t.setLawsImplied(t.getImpliesLaws().stream().map(this::getPositiveLaw).filter(Objects::nonNull).collect(Collectors.toSet()));
            }
        }
        for (Law t : negativeLaws.values()) {
            if (t.getImpliesLaws() == null) {
                t.setLawsImplied(List.of());
            } else {
                t.setLawsImplied(t.getImpliesLaws().stream().map(this::getNegativeLaw).filter(Objects::nonNull).collect(Collectors.toSet()));
            }
        }

        // set direct child Laws to Laws
        var allLaws = Stream.concat(getPositiveLaws().stream(), getNegativeLaws().stream()).collect(Collectors.toSet());
        for (Law law : allLaws) {
            if (law.getLawsImplied() == null)
                continue;
            for (Law base : law.getLawsImplied()) {
                if (base.getChildLaws() == null) {
                    base.setChildLaws(new HashSet<>());
                }
                base.getChildLaws().add(law);
            }
        }

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

    /**
     * Get domain-defined backend id, which determines the backend used to JUDGE this domain's questions. By default, the same as solving domain.
     */
    @NotNull
    public String getJudgingBackendId(/* TODO: pass question type ??*/){
        return this.getSolvingBackendId();
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
    protected abstract List<Question> getQuestionTemplates();

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
    public Question findQuestion(List<Tag> tags, Set<String> targetConcepts, Set<String> deniedConcepts, Set<String> targetNegativeLaws, Set<String> deniedNegativeLaws, Set<String> forbiddenQuestions) {
        List<Question> questions = new ArrayList<>();

        int maxSuitCount = 0;
        int minAdditionalCount = 10000;
        for (Question q : getQuestionTemplates()) {
            int targetConceptCount = 0;
            int anotherConcepts = 0;
            boolean suit = true;
            if (forbiddenQuestions.contains(q.getQuestionName()) || forbiddenQuestions.contains(NAME_PREFIX_IS_HUMAN + q.getQuestionName())) {
                continue;
            }
            for (Tag tag : tags) {
                if (!q.getTags().contains(tag.getName())) {
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
        } else {
            for (Question question : questions) {
                log.info("Отобранный вопрос (из {}): {}", questions.size(), question.getQuestionName());
            }

            Question question = questions.get(randomProvider.getRandom().nextInt(questions.size()));
            log.info("В итоге, взят вопрос: {}", question.getQuestionName());

            ///
            /// add a mark to the question's name: this question is made by human.
            if (question.getQuestionName() != null && ! question.getQuestionName().startsWith(NAME_PREFIX_IS_HUMAN) ) {
                question.getQuestionData().setQuestionName(NAME_PREFIX_IS_HUMAN + question.getQuestionName());
            }
            ///

            return question;
        }
    }

    //-----

    /**
     * Define all the {@link DomainToBackendAdapter} this domain supports
     */
    protected Set<DomainToBackendAdapter<?, ?, ?>> createBackendAdapters(){
        return Set.of(
            new FactBackend.Interface<>(JenaBackend.class, this, "Jena")
        );
    }

    private final Map<String, DomainToBackendAdapter<?, ?, ?>> backendClassToInterfaceMap =
        createBackendAdapters().stream()
            .collect(Collectors.toMap(
                DomainToBackendAdapter::getBackendId,
                Function.identity()
            ));

    /**
     * Get an interface instance which this Domain uses to interact with a given backend <br>
     * Returns null if no such interface exists
     * - however, this situation should not be possible if the system is working correctly
     */
    public final DomainToBackendAdapter<?, ?, ?> getBackendInterface(String backendId){
        return backendClassToInterfaceMap.get(backendId);
    }
}
