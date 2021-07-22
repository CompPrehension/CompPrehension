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

    public List<BackendFactEntity> getFacts() {
        return facts;
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


    public FactsGraph() {
        initEmptyFields();
    }

    public FactsGraph(List<BackendFactEntity> initialFacts) {
        initEmptyFields();
        // facts = new ArrayList<>();
        addFacts(initialFacts);
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
     * @param s subject
     * @param propertyChain list of properties (verbs)
     * @return list of objects.
     */
    public ArrayList<String> chainReachable(String s, List<String> propertyChain) {
        ArrayList<String> currentSubjs = new ArrayList<String>(List.of(s));
        // for each prop, sequentially ...
        for (String p : propertyChain) {
            // find what subjects are reachable from current set along `p`
            HashSet<String> nextSubjs = new HashSet<String>();
            for (String cs : currentSubjs) {
                List<BackendFactEntity> suitableFacts = filterFacts(cs, p, null);
                if (suitableFacts == null) continue;
                for (BackendFactEntity f : suitableFacts) {
                    nextSubjs.add(f.getObject());
                }
            }
            currentSubjs.clear();
            currentSubjs.addAll(nextSubjs);
            if (currentSubjs.isEmpty()) break;
        }
        return currentSubjs;
    }



}
