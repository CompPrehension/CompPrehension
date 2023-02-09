package org.vstu.compprehension.models.businesslogic.backend.facts;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.jena.rdf.model.*;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.businesslogic.backend.util.TermMapping;
import org.vstu.compprehension.models.entities.BackendFactEntity;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class JenaFactList implements Collection<Fact> {

    final static String BASE_URI_DEFAULT = "http://vstu.ru/poas/code#";

    protected transient List<JenaFact> list = null;
    protected transient boolean listIsUpToDate;

    @Getter
    protected transient TermMapping termMapping;

    @Getter
    protected transient Model model;

    public JenaFactList() {
        this(BASE_URI_DEFAULT);
    }

    public JenaFactList(String baseNSPrefix) {
        termMapping = new TermMapping(baseNSPrefix);
        initModel();
    }

    /** Using a copy of given model
     * @param model source model
     */
    public JenaFactList(Model model) {
        this();
        setModelCopy(model);
    }

    public JenaFactList(Collection<? extends Fact> facts) {
        this();
        if (facts instanceof JenaFactList) {
            // copy other
            setModelCopy(((JenaFactList)facts).model);
        } else {
            this.addAll(facts);
        }
    }

    /*public JenaFactList(Collection<? extends BackendFactEntity> facts) {
        this();
        this.addBackendFacts(facts);
    }*/

    public static JenaFactList fromBackendFacts(Collection<? extends BackendFactEntity> facts) {
        JenaFactList fl = new JenaFactList();
        fl.addBackendFacts(facts);
        return fl;
    }
    public static JenaFactList fromFacts(Collection<? extends Fact> facts) {
        if (facts instanceof JenaFactList) {
            // no need to convert
            return (JenaFactList) facts;
        } else {
            JenaFactList fl = new JenaFactList();
            fl.addAll(facts);
            return fl;
        }
    }

    @Override
    public String toString() {
        return "FactList{" +
                "model of size: " + model.size() +
                '}';
    }

    /** Set a copy of model
     * @param model source model
     */
    public void setModelCopy(Model model) {
        if (model != null) {
            this.model = ModelFactory.createDefaultModel();
            /*this.model = model;*/
            this.model.add(model); // make a copy
            list.clear();
            listIsUpToDate = false;
        } else
            initModel();
    }

    /** Set model by reference (thus it can be changed via updates of this list)
     * @param model updatable model
     */
    public void setModel(Model model) {
        if (model != null) {
            this.model = model;
            list.clear();
            listIsUpToDate = false;
        } else
            initModel();
    }

    private void initModel() {
        this.list = new ArrayList<>();

        this.model = ModelFactory.createDefaultModel();
        model.setNsPrefixes(termMapping.getModel().getNsPrefixMap());
        // model.setNsPrefix("my", termMapping.baseNSPrefix);
        listIsUpToDate = true;
    }

    /** Answer FactTriple (making a new instance if required) bound to this fact list
     * @param fact source fact
     * @param stmt statement to bound to
     * @return prepared FactTriple instance
     */
    public JenaFact bindFact(Fact fact, Statement stmt) {
        JenaFact ft;
        if (fact instanceof JenaFact) {
            ft = ((JenaFact) fact);
            ft.setStatement(stmt);
        } else {
            ft = new JenaFact(fact, stmt);
        }
        ft.setFactList(this);
        return ft;
    }

    private void fillList() {
        // assuming that `listIsUpToDate` is 0
        list.clear();
        model.listStatements().toList().forEach(st -> list.add(stmtToFact(st)));
        listIsUpToDate = true;
    }

    /** return bound fact for a statement */
    private JenaFact stmtToFact(Statement stmt) {
        JenaFact fact = new JenaFact(stmt);
        fact.setFactList(this);
        return fact;
    }

    /** Cretae statement for a fact, do not add it to the model explicitly.
     * @param fact source fact
     * @return new statement
     */
    private Statement factToStmt(Fact fact) {
        assert fact != null;
        return JenaFact.makeStatement(fact, model, termMapping);
    }


    /**
     * Returns the number of elements in this list.
     * @return the number of elements in this list
     */
    @Override
    public int size() {
        return (int) model.size();
    }

    /**
     * Returns {@code true} if this list contains no elements.
     *
     * @return {@code true} if this list contains no elements
     */
    @Override
    public boolean isEmpty() {
        return model.isEmpty();
    }

    /**
     * Returns {@code true} if this list contains the specified element.
     * More formally, returns {@code true} if and only if this list contains
     * at least one element {@code e} such that
     * {@code Objects.equals(o, e)}.
     *
     * @param o element whose presence in this list is to be tested
     * @return {@code true} if this list contains the specified element
     * @throws ClassCastException   if the type of the specified element
     *                              is incompatible with this list
     *                              (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null and this
     *                              list does not permit null elements
     *                              (<a href="Collection.html#optional-restrictions">optional</a>)
     */
    @Override
    public boolean contains(Object o) {
        if (o instanceof Statement) {
            return model.contains((Statement) o);
        }
        if (o instanceof Fact) {
            return model.contains(factToStmt((Fact) o));
        }

        return false;
    }

    /**
     * Returns an iterator over the elements in this list in proper sequence.
     *
     * @return an iterator over the elements in this list in proper sequence
     */
    @NotNull
    @Override
    public Iterator<Fact> iterator() {
        if (listIsUpToDate)
            return wrapListIterator(list.listIterator());

        StmtIterator it = model.listStatements();
        return new FactIterator(it);
    }

    /**
     * Returns an array containing all of the elements in this list in proper
     * sequence (from first to last element).
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this list.  (In other words, this method must
     * allocate a new array even if this list is backed by an array).
     * The caller is thus free to modify the returned array.
     *
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all of the elements in this list in proper
     * sequence
     * @see Arrays#asList(Object[])
     */
    @NotNull
    @Override
    public Object[] toArray() {
        if (!listIsUpToDate) {
            fillList();
        }
        return list.toArray();
    }

    /**
     * Returns an array containing all of the elements in this list in
     * proper sequence (from first to last element); the runtime type of
     * the returned array is that of the specified array.  If the list fits
     * in the specified array, it is returned therein.  Otherwise, a new
     * array is allocated with the runtime type of the specified array and
     * the size of this list.
     *
     * <p>If the list fits in the specified array with room to spare (i.e.,
     * the array has more elements than the list), the element in the array
     * immediately following the end of the list is set to {@code null}.
     * (This is useful in determining the length of the list <i>only</i> if
     * the caller knows that the list does not contain any null elements.)
     *
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     *
     * <p>Suppose {@code x} is a list known to contain only strings.
     * The following code can be used to dump the list into a newly
     * allocated array of {@code String}:
     *
     * <pre>{@code
     *     String[] y = x.toArray(new String[0]);
     * }</pre>
     * <p>
     * Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
     *
     * @param a the array into which the elements of this list are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose.
     * @return an array containing the elements of this list
     * @throws ArrayStoreException  if the runtime type of the specified array
     *                              is not a supertype of the runtime type of every element in
     *                              this list
     * @throws NullPointerException if the specified array is null
     */
    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        if (!listIsUpToDate) {
            fillList();
        }
        return list.toArray(a);
    }

    /**
     * Appends the specified element to the end of this list (optional
     * operation).
     *
     * <p>Lists that support this operation may place limitations on what
     * elements may be added to this list.  In particular, some
     * lists will refuse to add null elements, and others will impose
     * restrictions on the type of elements that may be added.  List
     * classes should clearly specify in their documentation any restrictions
     * on what elements may be added.
     *
     * @param factTriple element to be appended to this list
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws UnsupportedOperationException if the {@code add} operation
     *                                       is not supported by this list
     * @throws ClassCastException            if the class of the specified element
     *                                       prevents it from being added to this list
     * @throws NullPointerException          if the specified element is null and this
     *                                       list does not permit null elements
     * @throws IllegalArgumentException      if some property of this element
     *                                       prevents it from being added to this list
     */
    @Override
    public boolean add(Fact factTriple) {
        Statement st = factToStmt(factTriple);
        model.add(st);
        listIsUpToDate = false;
        return true;
    }

    /**
     * @param otherModel other model containing statements to add
     * @return true if this model changed
     */
    public boolean addFromModel(Model otherModel) {
        long oldSize = model.size();
        model.add(otherModel);
        listIsUpToDate = false;
        return oldSize != model.size();
    }

    /**
     * Removes the first occurrence of the specified element from this list,
     * if it is present (optional operation).  If this list does not contain
     * the element, it is unchanged.  More formally, removes the element with
     * the lowest index {@code i} such that
     * {@code Objects.equals(o, get(i))}
     * (if such an element exists).  Returns {@code true} if this list
     * contained the specified element (or equivalently, if this list changed
     * as a result of the call).
     *
     * @param o element to be removed from this list, if present
     * @return {@code true} if this list contained the specified element
     * @throws ClassCastException            if the type of the specified element
     *                                       is incompatible with this list
     *                                       (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException          if the specified element is null and this
     *                                       list does not permit null elements
     *                                       (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws UnsupportedOperationException if the {@code remove} operation
     *                                       is not supported by this list
     */
    @Override
    public boolean remove(Object o) {
        if (o instanceof Statement) {
            Statement st = (Statement) o;
            model.remove(st);
            if (listIsUpToDate)
                for (JenaFact ft : list) {
                    if (ft.getStatement().equals(st)) {
                        list.remove(ft);
                        break;
                    }
                }
            return true;
        }
        if (o instanceof Fact) {
            Statement st = factToStmt((Fact) o);
            model.remove(st);
            if (listIsUpToDate && !list.remove(o)) {
                for (JenaFact ft : list)
                    if (ft.getStatement().equals(st)) {
                        list.remove(ft);
                        break;
                    }
            }
            return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if this list contains all of the elements of the
     * specified collection.
     *
     * @param c collection to be checked for containment in this list
     * @return {@code true} if this list contains all of the elements of the
     * specified collection
     * @throws ClassCastException   if the types of one or more elements
     *                              in the specified collection are incompatible with this
     *                              list
     *                              (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified collection contains one
     *                              or more null elements and this list does not permit null
     *                              elements
     *                              (<a href="Collection.html#optional-restrictions">optional</a>),
     *                              or if the specified collection is null
     * @see #contains(Object)
     */
    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return false;
    }

    /**
     * Appends all of the elements in the specified collection to the end of
     * this list, in the order that they are returned by the specified
     * collection's iterator (optional operation).  The behavior of this
     * operation is undefined if the specified collection is modified while
     * the operation is in progress.  (Note that this will occur if the
     * specified collection is this list, and it's nonempty.)
     *
     * @param c collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws UnsupportedOperationException if the {@code addAll} operation
     *                                       is not supported by this list
     * @throws ClassCastException            if the class of an element of the specified
     *                                       collection prevents it from being added to this list
     * @throws NullPointerException          if the specified collection contains one
     *                                       or more null elements and this list does not permit null
     *                                       elements, or if the specified collection is null
     * @throws IllegalArgumentException      if some property of an element of the
     *                                       specified collection prevents it from being added to this list
     * @see #add(Fact)
     */
    @Override
    public boolean addAll(@NotNull Collection<? extends Fact> c) {
        if (c instanceof JenaFactList) {
            // adding other
            JenaFactList other = (JenaFactList) c;
            addFromModel(other.model);
        } else {
            for (Fact el : c) {
                add(el);
            }
        }
        return true;
    }
    public boolean addBackendFacts(@NotNull Collection<? extends BackendFactEntity> c) {
        for (BackendFactEntity el : c) {
            add(new JenaFact(el));
        }
        return true;
    }

    /**
     * Removes from this list all of its elements that are contained in the
     * specified collection (optional operation).
     *
     * @param c collection containing elements to be removed from this list
     * @return {@code true} if this list changed as a result of the call
     * @throws UnsupportedOperationException if the {@code removeAll} operation
     *                                       is not supported by this list
     * @throws ClassCastException            if the class of an element of this list
     *                                       is incompatible with the specified collection
     *                                       (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException          if this list contains a null element and the
     *                                       specified collection does not permit null elements
     *                                       (<a href="Collection.html#optional-restrictions">optional</a>),
     *                                       or if the specified collection is null
     * @see #remove(Object)
     * @see #contains(Object)
     */
    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        boolean changed = false;
        for (Object el : c) {
            changed |= remove(el);
        }
        return changed;
    }

    /**
     * Retains only the elements in this list that are contained in the
     * specified collection (optional operation).  In other words, removes
     * from this list all of its elements that are not contained in the
     * specified collection.
     *
     * @param c collection containing elements to be retained in this list
     * @return {@code true} if this list changed as a result of the call
     * @throws UnsupportedOperationException if the {@code retainAll} operation
     *                                       is not supported by this list
     * @throws ClassCastException            if the class of an element of this list
     *                                       is incompatible with the specified collection
     *                                       (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException          if this list contains a null element and the
     *                                       specified collection does not permit null elements
     *                                       (<a href="Collection.html#optional-restrictions">optional</a>),
     *                                       or if the specified collection is null
     * @see #remove(Object)
     * @see #contains(Object)
     */
    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        Model oldModel = model;
        initModel(); // creates new this.model and this.list
        for (Object el : c) {
            if (el instanceof Fact) {
                Fact factTriple = (Fact) el;
                Statement st = factToStmt(factTriple);
                if (oldModel.contains(st)) {
                    model.add(st);
                    list.add(bindFact(factTriple, st));  // fill list from empty state
                }
            }
        }
        oldModel.close(); // clear
        return true;  // always true as content re-created
    }

    /**
     * Removes all of the elements from this list (optional operation).
     * The list will be empty after this call returns.
     *
     * @throws UnsupportedOperationException if the {@code clear} operation
     *                                       is not supported by this list
     */
    @Override
    public void clear() {
        initModel();
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   ({@code index < 0 || index >= size()})
     */
    public JenaFact get(int index) {
        if (!listIsUpToDate) {
            fillList();
        }
        return list.get(index);
    }


    /*@NotNull
    @Override
    public ListIterator<Fact> listIterator() {
        return listIterator(0);
    }*/

    /*@NotNull
    @Override
    public ListIterator<Fact> listIterator(int index) {
        if (!listIsUpToDate)
            fillList();
        return wrapListIterator(list.listIterator(index));
    }*/

    @NotNull
    private ListIterator<Fact> wrapListIterator(ListIterator<JenaFact> it_0) {
        JenaFactList factList = this;
        return new ListIterator<>() {
            ListIterator<JenaFact> it = it_0;

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Fact next() {
                return it.next();
            }

            @Override
            public boolean hasPrevious() {
                return it.hasPrevious();
            }

            @Override
            public Fact previous() {
                return it.previous();
            }

            @Override
            public int nextIndex() {
                return it.nextIndex();
            }

            @Override
            public int previousIndex() {
                return it.previousIndex();
            }

            @Override
            public void remove() {
                /*it.remove();*/ // we don't know the previous operation and thus the index of element to remove (since we have no access to private part of `it`).
            }

            @Override
            public void set(Fact backendFactEntity) {
                /*it.set(backendFactEntity);*/
            }

            @Override
            public void add(Fact backendFactEntity) {
                factList.add(/*it.nextIndex(),*/ backendFactEntity);
                it = factList.list.listIterator(it.nextIndex());
            }
        };
    }

    /*@NotNull
    @Override
    public List<Fact> subList(int fromIndex, int toIndex) {
        if (!listIsUpToDate) {
            fillList();
        }
        return list.subList(fromIndex, toIndex).stream()
                .map(t -> (Fact)t)
                .collect(Collectors.toList());
    }*/

    public List<BackendFactEntity> asBackendFactList() {
        if (!listIsUpToDate) {
            fillList();
        }
        return list.stream()
                .map(JenaFact::asBackendFact)
                .collect(Collectors.toList());
    }


    class FactIterator implements Iterator<Fact> {
        StmtIterator it;

        public FactIterator(StmtIterator it) {
            this.it = it;
        }

        /**
             * Returns {@code true} if the iteration has more elements.
             * (In other words, returns {@code true} if {@link #next} would
             * return an element rather than throwing an exception.)
             *
             * @return {@code true} if the iteration has more elements
             */
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            /**
             * Returns the next element in the iteration.
             *
             * @return the next element in the iteration
             * @throws NoSuchElementException if the iteration has no more elements
             */
            @Override
            public Fact next() {
                Statement stmt = it.next();
                return stmtToFact(stmt);
            }
    }

}
