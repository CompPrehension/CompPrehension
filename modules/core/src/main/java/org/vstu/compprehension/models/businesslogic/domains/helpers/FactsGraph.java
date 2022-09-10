package org.vstu.compprehension.models.businesslogic.domains.helpers;

import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.models.entities.BackendFactEntity;

import java.util.*;

public class FactsGraph {
    private List<BackendFactEntity> facts;

    // indexing facts by S,P,O
    private HashMap<String, List<BackendFactEntity>> subj2fs;
    private HashMap<String, List<BackendFactEntity>> prop2fs;
    private HashMap<String, List<BackendFactEntity>> obj2fs;

    public FactsGraph() {
        initEmptyFields();
    }

    public FactsGraph(List<BackendFactEntity> initialFacts) {
        initEmptyFields();
        addFacts(initialFacts);
    }

    public List<BackendFactEntity> getFacts() {
        return new ArrayList<>(facts);
    }

    private void initEmptyFields() {
        facts = new ArrayList<>();
        subj2fs = new HashMap<>();
        prop2fs = new HashMap<>();
        obj2fs = new HashMap<>();
    }

    private static void add2Map(HashMap<String, List<BackendFactEntity>> map, String key, BackendFactEntity value) {
        if (map.containsKey(key)) {
            map.get(key).add(value);
        } else {
            List<BackendFactEntity> list = new ArrayList<>();
            list.add(value);
            map.put(key, list);
        }
    }

    private static void removeFromMap(HashMap<String, List<BackendFactEntity>> map, String key, BackendFactEntity value) {
        if (map.containsKey(key)) {
            List<BackendFactEntity> list = map.get(key);
            list.remove(value);
            if (list.isEmpty()) {
                map.remove(key);
            }
        }
    }

    public void addFact(BackendFactEntity newFact) {
        facts.add(newFact);

        BackendFactEntity f = newFact;
        String key = f.getSubject();
        add2Map(subj2fs, key, f);
        key = f.getVerb();
        add2Map(prop2fs, key, f);
        key = f.getObject();
        add2Map(obj2fs, key, f);
    }

    public void addFacts(List<BackendFactEntity> newFacts) {
        facts.addAll(newFacts);

        for(BackendFactEntity f : newFacts) {
            String key = f.getSubject();
            add2Map(subj2fs, key, f);
            key = f.getVerb();
            add2Map(prop2fs, key, f);
            key = f.getObject();
            add2Map(obj2fs, key, f);
        }
    }

    /**
     * Remove exact BackendFactEntity objects.
     * @param extraFacts
     */
    public void removeFacts(List<BackendFactEntity> extraFacts) {
        facts.removeAll(extraFacts);

        for(BackendFactEntity f : extraFacts) {
            String key = f.getSubject();
            removeFromMap(subj2fs, key, f);
            key = f.getVerb();
            removeFromMap(prop2fs, key, f);
            key = f.getObject();
            removeFromMap(obj2fs, key, f);
        }
    }

    public HashMap<Integer, Integer> describeDuplicates() {
        HashMap<String, Integer> f2count = new HashMap<>();

        for(BackendFactEntity f : facts) {
            String s = f.getSubject();
            String p = f.getVerb();
            String o = f.getObject();
            String mapKey = s + p + o;

            if (! f2count.containsKey(mapKey))
                f2count.put(mapKey, filterFacts(s, p, o).size());
        }

        // reverse {fact -> count}  to  {count -> Number-of-facts}
        HashMap<Integer, Integer> count2n = new HashMap<>();
        for (Integer count : f2count.values()) {
            count2n.put(count, 1 + count2n.getOrDefault(count, 0));
        }

        /// debug print it
        System.out.println(count2n.toString() + " = " + facts.size() + " total.");
        ///

        return count2n;
    }

    public FactsGraph removeDuplicates() {
        for(BackendFactEntity f : new ArrayList<>(facts)) {
            String s = f.getSubject();
            String p = f.getVerb();
            String o = f.getObject();

            List<BackendFactEntity> list = filterFacts(s, p, o);
            if (list.size() > 1) {
                list.remove(0); // keep exactly one
                removeFacts(list);
            }
        }

        return this;
    }

    /**
     * Remove facts that have Subject, Predicate and Object equal (the fact objects itself can be different)
     * @param undesirableFacts
     * @return
     */
    public FactsGraph removeAllLike(List<BackendFactEntity> undesirableFacts) {
        for(BackendFactEntity f : undesirableFacts) {
            String s = f.getSubject();
            String p = f.getVerb();
            String o = f.getObject();

            List<BackendFactEntity> list = filterFacts(s, p, o);
            if (list.size() > 0) {
                removeFacts(list);
            }
        }

        return this;
    }

    public List<BackendFactEntity> filterFacts(@Nullable String s, @Nullable String p, @Nullable String o) {
//        Set<BackendFactEntity> candidates = new HashSet<>();
        List<BackendFactEntity> candidates = new ArrayList<>();
        if (p != null) {
            List<BackendFactEntity> indexed = prop2fs.get(p);
            if (indexed == null) {
                return candidates; // empty
            }
            candidates.addAll(indexed);
        }
        if (s != null) {
            List<BackendFactEntity> indexed = subj2fs.get(s);
            if (indexed == null) { // no such subjects
                candidates.clear();
                return candidates; // empty
            }
            if (p == null)
                candidates.addAll(indexed);
            else
                candidates.retainAll(indexed);
        }
        if (o != null) {
            List<BackendFactEntity> indexed = obj2fs.get(o);
            if (indexed == null) {
                candidates.clear();
                return candidates; // empty
            }
            if (p == null && s == null)
                candidates.addAll(indexed);
            else
                candidates.retainAll(indexed);
        }
        return candidates;
    }

    public List<BackendFactEntity> findFactsLike(BackendFactEntity fact) {
        return filterFacts(fact.getSubject(), fact.getVerb(), fact.getObject());
    }

    public boolean factExists(@Nullable String s, @Nullable String p, @Nullable String o) {
        return ! filterFacts(s, p, o).isEmpty();
    }

    public BackendFactEntity findOne(@Nullable String s, @Nullable String p, @Nullable String o) {
        List<BackendFactEntity> candidates = filterFacts(s, p, o);
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    /**
     * Check if a fixed chain of properties does exist between not-null subject and object.
     * @param s subject
     * @param propertyChain list of properties (verbs)
     * @param o object
     * @return true if chain exists in graph.
     */
    public boolean chainExists(String s, List<String> propertyChain, String o) {
        ArrayList<String> objects = chainReachable(s, propertyChain);
        // check if `o` is in `currentSubjs`
        for (String cs : objects) {
            if (Objects.equals(cs, o))
                return true;
        }
        return false;
    }

    /**
     * Get objects reachable from given subject via a fixed chain of properties.
     * A property can be inverted with "^" prefix
     * @param s subject
     * @param propertyChain list of properties (verbs)
     * @return list of objects.
     */
    public ArrayList<String> chainReachable(String s, List<String> propertyChain) {
        ArrayList<String> currentSubjs = new ArrayList<>(List.of(s));
        // for each prop, sequentially ...
        for (String p : propertyChain) {
            boolean inverse = p.startsWith("^");
            if (inverse) {
                p = p.substring(1);
            }
            // find what subjects are reachable from current set along `p`
            HashSet<String> nextSubjs = new HashSet<>();
            for (String cs : currentSubjs) {
                List<BackendFactEntity> suitableFacts;
                if (!inverse) {
                    suitableFacts = filterFacts(cs, p, null);
                    if (suitableFacts == null) continue;
                    for (BackendFactEntity f : suitableFacts) {
                        nextSubjs.add(f.getObject());
                    }
                } else {
                    suitableFacts = filterFacts(null, p, cs);
                    if (suitableFacts == null) continue;
                    for (BackendFactEntity f : suitableFacts) {
                        nextSubjs.add(f.getSubject());
                    }
                }
            }
            currentSubjs.clear();
            currentSubjs.addAll(nextSubjs);
            if (currentSubjs.isEmpty()) break;
        }
        return currentSubjs;
    }


    public static ArrayList<BackendFactEntity> factsListDeepCopy(List<BackendFactEntity> list) {
        ArrayList<BackendFactEntity> result = new ArrayList<>();
        // re-create each fact
        for (BackendFactEntity f : list) {
            result.add(new BackendFactEntity(
                    f.getSubjectType(),
                    f.getSubject(),
                    f.getVerb(),
                    f.getObjectType(),
                    f.getObject()
            ));
        }

        return result;
    }

    /**
     * Add new facts to the list and ignore the ones that exist.
     * @param list the list being updated
     * @param newFacts collection of facts can be appended
     */
    public static void updateFactsList(List<BackendFactEntity> list, List<BackendFactEntity> newFacts) {
        FactsGraph fg = new FactsGraph(list);

        for (BackendFactEntity fact : newFacts) {
            if (fg.findFactsLike(fact).isEmpty()) {
                list.add(fact);
                fg.addFact(fact);
            }
        }
    }
}
