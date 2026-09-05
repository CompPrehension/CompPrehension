package org.vstu.compprehension.models.businesslogic.domains.helpers;

import org.vstu.compprehension.models.data.BackendFactData;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FactsGraph {
    private List<BackendFactData> facts;

    // indexing facts by S,P,O
    private HashMap<String, List<BackendFactData>> subj2fs;
    private HashMap<String, List<BackendFactData>> prop2fs;
    private HashMap<String, List<BackendFactData>> obj2fs;

    public FactsGraph() {
        initEmptyFields();
    }

    public FactsGraph(List<BackendFactData> initialFacts) {
        initEmptyFields();
        addFacts(initialFacts);
    }

    public List<BackendFactData> getFacts() {
        return new ArrayList<>(facts);
    }

    public List<BackendFactData> getFactsAsIs() {
        return facts;
    }

    private void initEmptyFields() {
        facts = new ArrayList<>();
        subj2fs = new HashMap<>();
        prop2fs = new HashMap<>();
        obj2fs = new HashMap<>();
    }

    private static void add2Map(HashMap<String, List<BackendFactData>> map, String key, BackendFactData value) {
        if (map.containsKey(key)) {
            map.get(key).add(value);
        } else {
            List<BackendFactData> list = new ArrayList<>();
            list.add(value);
            map.put(key, list);
        }
    }

    private static void removeFromMap(HashMap<String, List<BackendFactData>> map, String key, BackendFactData value) {
        if (map.containsKey(key)) {
            List<BackendFactData> list = map.get(key);
            list.remove(value);
            if (list.isEmpty()) {
                map.remove(key);
            }
        }
    }

    public void addFact(BackendFactData newFact) {
        facts.add(newFact);

        BackendFactData f = newFact;
        String key = f.getSubject();
        add2Map(subj2fs, key, f);
        key = f.getVerb();
        add2Map(prop2fs, key, f);
        key = f.getObject();
        add2Map(obj2fs, key, f);
    }

    public void addFacts(List<BackendFactData> newFacts) {
        facts.addAll(newFacts);

        for(BackendFactData f : newFacts) {
            String key = f.getSubject();
            add2Map(subj2fs, key, f);
            key = f.getVerb();
            add2Map(prop2fs, key, f);
            key = f.getObject();
            add2Map(obj2fs, key, f);
        }
    }

    /**
     * Remove exact BackendFactData objects.
     * @param extraFacts
     */
    public void removeFacts(List<BackendFactData> extraFacts) {
        facts.removeAll(extraFacts);

        for(BackendFactData f : extraFacts) {
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

        for(BackendFactData f : facts) {
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
        // System.out.println(count2n.toString() + " = " + facts.size() + " total.");
        ///

        return count2n;
    }

    public FactsGraph removeDuplicates() {
        for(BackendFactData f : new ArrayList<>(facts)) {
            String s = f.getSubject();
            String p = f.getVerb();
            String o = f.getObject();

            List<BackendFactData> list = filterFacts(s, p, o);
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
     * @return this, for chaining
     */
    public FactsGraph removeAllLike(List<BackendFactData> undesirableFacts) {
        for(BackendFactData f : undesirableFacts) {
            String s = f.getSubject();
            String p = f.getVerb();
            String o = f.getObject();

            List<BackendFactData> list = filterFacts(s, p, o);
            if (list.size() > 0) {
                removeFacts(list);
            }
        }

        return this;
    }

    public List<BackendFactData> filterFacts(@Nullable String s, @Nullable String p, @Nullable String o) {
//        Set<BackendFactData> candidates = new HashSet<>();
        List<BackendFactData> candidates = new ArrayList<>();
        if (p != null) {
            List<BackendFactData> indexed = prop2fs.get(p);
            if (indexed == null) {
                return candidates; // empty
            }
            candidates.addAll(indexed);
        }
        if (s != null) {
            List<BackendFactData> indexed = subj2fs.get(s);
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
            List<BackendFactData> indexed = obj2fs.get(o);
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

    public List<BackendFactData> findFactsLike(BackendFactData fact) {
        return filterFacts(fact.getSubject(), fact.getVerb(), fact.getObject());
    }

    public boolean factExists(@Nullable String s, @Nullable String p, @Nullable String o) {
        return ! filterFacts(s, p, o).isEmpty();
    }

    public BackendFactData findOne(@Nullable String s, @Nullable String p, @Nullable String o) {
        List<BackendFactData> candidates = filterFacts(s, p, o);
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
                List<BackendFactData> suitableFacts;
                if (!inverse) {
                    suitableFacts = filterFacts(cs, p, null);
                    if (suitableFacts == null) continue;
                    for (BackendFactData f : suitableFacts) {
                        nextSubjs.add(f.getObject());
                    }
                } else {
                    suitableFacts = filterFacts(null, p, cs);
                    if (suitableFacts == null) continue;
                    for (BackendFactData f : suitableFacts) {
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


    public static ArrayList<BackendFactData> factsListDeepCopy(List<BackendFactData> list) {
        ArrayList<BackendFactData> result = new ArrayList<>();
        // re-create each fact
        for (BackendFactData f : list) {
            result.add(new BackendFactData(
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
    public static void updateFactsList(List<BackendFactData> list, List<BackendFactData> newFacts) {
        FactsGraph fg = new FactsGraph(list);

        for (BackendFactData fact : newFacts) {
            if (fg.findFactsLike(fact).isEmpty()) {
                list.add(fact);
                fg.addFact(fact);
            }
        }
    }
}
