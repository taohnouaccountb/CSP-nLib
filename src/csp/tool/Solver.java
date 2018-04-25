/*
 * CSCE421 2018Spr
 * Tao Yao
 * 09157432
 * Jan. 21, 2018
 * */
package csp.tool;

import csp.MyParser;
import csp.data.Constraint;
import csp.data.Variable;
import csp.data.simpleVariable;
import csp.tool.bt.dynamicVariableChooser;
import csp.tool.bt.staticVariableChooser;
import csp.tool.bt.variableChooser;
import csp.tool.bt.variableChooser.heuristicType;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Solver {
    private csp.data.Problem problem = null;
    private int time_setup = -1;
    private int time_working = -1;
    private int check_counts = 0; //cc
    private long cpu_time = 0;
    private int remove_counts = 0; //fval

    private Boolean finishedFlag = false;

    private double initial_size = -1;

    public Solver() {}

    public double getInitial_size() {
        if (problem == null) System.out.println("ERROR: Solver has not been initialized.");
        if (initial_size == -1) {
            initial_size = calc_size();
            return initial_size;
        } else {
            return initial_size;
        }
    }

    public double getFiltered_size() {
        return calc_size();
    }

    private double calc_size() {
        /*boolean flag = false;
        long ans = 1;
        for (simpleVariable i : variables) {
            if (ans > 1e9) {
                flag = true;
                break;
            }
            ans *= i.getCurrent_domain().size();
        }
        if (flag == false) {
            return ans;
        }*/
        double dans = 0;
        for (simpleVariable i : variables) {
            dans += java.lang.Math.log((double) i.getCurrent_domain().size());
        }
        return dans;
    }

    public double filterEffect() {
        double ans = variables.parallelStream().map(i -> (double) (i.getCurrent_domain().size()) / i.getRefVar().getInitial_domain().length)
                .reduce((i, j) -> i * j).get();
        return 1 - ans;
    }

    private List<simpleVariable> variables = null;
    private List<Constraint> constraints = null;
    private Map<Variable, simpleVariable> findSimpleVariable = null;

    public void init(csp.data.Problem problem) {
        this.problem = problem;

        findSimpleVariable = new HashMap<>();
        variables = new ArrayList<>();
        problem.variables.forEach(i -> {
            simpleVariable n = new simpleVariable(i);
            variables.add(n);
            findSimpleVariable.put(i, n);
        });

        constraints = new ArrayList<Constraint>();
        constraints.addAll(problem.constraints);

        getInitial_size();

    }

    public int cutDomainByList(LinkedList<Integer> target, List<Boolean> flag_list) throws NoSolutionException {
        int count = 0;
        Iterator<Integer> it = target.iterator();
        for (Boolean is_supported : flag_list) {
            it.next();
            if (!is_supported) {
                it.remove();
                count++;
                remove_counts++;
            }
        }
        if (target.size() == 0) {
            throw new NoSolutionException();
        }
        return count;
    }

    public boolean check(simpleVariable a, int va, simpleVariable b, int vb) {
        check_counts++;
        Boolean haveConflict = a.getRefVar().constraints.stream().filter(i -> i.getArity() == 2).anyMatch((i) -> {
            if (i.scope[0].getName().equals(a.getName()) && i.scope[1].getName().equals(b.getName())) {
                int[] tuple = {va, vb};
                try {
                    return !i.check(tuple);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (i.scope[1].getName().equals(a.getName()) && i.scope[0].getName().equals(b.getName())) {
                int[] tuple = {vb, va};
                try {
                    return !i.check(tuple);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return false;
        });
        return !haveConflict;
    }

    public boolean check(final simpleVariable a, final int va) {
        Optional<Boolean> flag = constraints.stream().filter(i -> i.getArity() == 1 && i.scope[0].getName().equals(a.getName()))
                .map((i) -> {
                    int tuple[] = {va};
                    try {
                        return i.check(tuple);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                }).filter(i -> i == false).findAny();
        return !flag.isPresent();
    }

    private Map<simpleVariable, Map<simpleVariable, Map<Integer, Integer>>> ac2001Track = null;
    private boolean ac2001_enable = false;

    public boolean support(final simpleVariable a, final int va, final simpleVariable b) {
        if (ac2001_enable) {
            Map<Integer, Integer> pair_ab = ac2001Track.get(a).get(b);
            if (pair_ab.get(va) != null) {
                int vb = pair_ab.get(va);
                if (vb == -1) {
                    return false;
                } else if (b.getCurrent_domain().parallelStream().anyMatch(i -> i == vb)) {
                    return true;
                }
            }

            Optional<Integer> x = b.getCurrent_domain().stream().filter(i -> check(a, va, b, i)).findAny();
            if (x.isPresent()) {
                pair_ab.put(va, x.get());
            } else {
                pair_ab.put(va, -1);
            }

            return pair_ab.get(va) != -1;
        }
        return b.getCurrent_domain().stream().anyMatch(i -> check(a, va, b, i));
    }

    public boolean revise(final simpleVariable a, final simpleVariable b) throws NoSolutionException {
        List<Boolean> rst_flag = a.getCurrent_domain().stream().map(va -> support(a, va, b)).collect(Collectors.toList());
        int cut_counts = cutDomainByList(a.getCurrent_domain(), rst_flag);
        return cut_counts > 0;
    }

    private Stream<solverSimpleVarPair> getInitQueue_stream() {
        return constraints.stream().filter(i -> i.getArity() == 2)
                .flatMap((i) -> {
                    //Double the pairs
                    simpleVariable a = findSimpleVariable.get(i.scope[0]);
                    simpleVariable b = findSimpleVariable.get(i.scope[1]);
                    return Stream.of(new solverSimpleVarPair(a, b), new solverSimpleVarPair(b, a));
                }).distinct();
    }

    private Queue<solverSimpleVarPair> getInitQueue_queue() {
        Queue<solverSimpleVarPair> q = new ConcurrentLinkedQueue<>();
        getInitQueue_stream().forEach(q::offer);
        return q;
    }

    private List<solverSimpleVarPair> getInitQueue_list() {
        return getInitQueue_stream().collect(Collectors.toList());
    }

    public enum SOLUTIONS_ac {AC1, AC3, NC, AC2001}

    public solverReporter_ac solve_ac(SOLUTIONS_ac x) {
        remove_counts = 0;
        long startTime = System.nanoTime();

        try {
            if (problem == null) {
                System.out.println("ERROR: Problem Uninitialized");
                throw new NoSolutionException();
            }
            if (x == SOLUTIONS_ac.AC1) {
                AC1();
            } else if (x == SOLUTIONS_ac.AC3) {
                AC3();
            } else if (x == SOLUTIONS_ac.NC) {
                NC();
            } else if (x == SOLUTIONS_ac.AC2001) {
                AC2001();
            } else {
                System.out.println("ERROR: Solution not exist");
            }
            finishedFlag = true;
            long cpu_time = (System.nanoTime() - startTime) / 1000000;
            return new solverReporter_ac(MyParser.file_name, problem.name, check_counts, cpu_time, remove_counts,
                    getInitial_size(), getFiltered_size(), filterEffect());
        } catch (NoSolutionException e) {
            return new solverReporter_ac(MyParser.file_name, problem.name, check_counts, cpu_time, remove_counts,
                    getInitial_size(), -1, -1);
        }
    }

    private void NC() throws NoSolutionException {
        for (simpleVariable i : variables) {
            List<Boolean> rst_flag = i.getCurrent_domain().stream().map(j -> check(i, j))
                    .collect(Collectors.toList());
            cutDomainByList(i.getCurrent_domain(), rst_flag);
        }
    }

    public void AC_trim() {
        variables.forEach(simpleVariable::reverseRestore);
    }

    private long firstTime = 0;
    private int firstCc = 0;
    private int firstNv = 0;
    private int firstBt = 0;
    private int nv = 0;
    private int bt = 0;

    public enum SOLUTIONS_bt {FC, MAC}

    private List<List<Integer>> search_solutions = null;

    public solverReporter_bt solve_bt(SOLUTIONS_bt x, heuristicType var_heuristic) {
        search_solutions = new ArrayList<>();
        remove_counts = 0;
        check_counts = 0;
        firstTime = 0;
        nv = 0;
        bt = 0;
        long startTime = System.nanoTime();

        isRelated = new relatedJudge(getInitQueue_stream());

        if (problem == null) {
            System.out.println("ERROR: Problem Uninitialized");
        }

        if (x == SOLUTIONS_bt.FC) {
            FC(var_heuristic);
        } else if (x == SOLUTIONS_bt.MAC) {
            MAC(var_heuristic);
        } else {
            System.out.println("ERROR: Solution not exist");
        }

        finishedFlag = true;
        long first_cpu_time = (firstTime - startTime) / 1000000;
        long cpu_time = (System.nanoTime() - startTime) / 1000000;
        if (search_solutions.size() == 0) {
            return new solverReporter_bt(chooser, problem.name, problem.BTtype,
                    var_heuristic.toString(), var_heuristic.name().startsWith("d") ? "dynamic" : "static",
                    "LX", "static",
                    firstCc, firstNv, firstBt, -1, new ArrayList<Integer>(),
                    check_counts, nv, bt, cpu_time, 0);
        }
        return new solverReporter_bt(chooser, problem.name, problem.BTtype,
                var_heuristic.toString(), var_heuristic.name().startsWith("d") ? "dynamic" : "static",
                "LX", "static",
                firstCc, firstNv, firstBt, first_cpu_time, search_solutions.get(0),
                check_counts, nv, bt, cpu_time, search_solutions.size());
    }

    private relatedJudge isRelated = null;
    private variableChooser chooser;

    private void FC(heuristicType mode) {
        boolean oneSolution = true;
        if (mode.name().startsWith("d")) {
            variables.forEach(simpleVariable::initFC);
            chooser = new dynamicVariableChooser(variables, mode, isRelated);
        } else {
            chooser = new staticVariableChooser(variables, mode, isRelated);
        }
        int var_num = chooser.getSize();
        int v[] = new int[var_num];

        int ith = 0;

        boolean consistent = true;
        while (ith > -1) {
//            System.out.println(ith);
            if (ith > chooser.getCurSize()) chooser.next();
            simpleVariable cur = chooser.get(ith);

            if (consistent) {
                assert chooser.get(ith).future_fc.isEmpty();
                //fc-label
                assert (!cur.getCurrent_domain().isEmpty());
                while (!cur.getCurrent_domain().isEmpty()) {
                    v[ith] = cur.getCurrent_domain().getFirst();
                    nv++;
                    boolean findWipeout = false;
                    List<simpleVariable> unused = chooser.getUnusedVariables(ith);
                    for (int k = 0; !findWipeout && k < unused.size(); k++) {
                        simpleVariable curr = unused.get(k);
                        int X=curr.getCurrent_domain().getLast();
                        if (isRelated.isExist(cur, curr)) {
                            //check-foward
                            Set<Integer> new_reduction = new TreeSet<>();
                            for (int m : curr.getCurrent_domain()) {
                                if (!check(cur, v[ith], curr, m)) {
                                    new_reduction.add(m);
                                }
                            }
                            curr.getCurrent_domain().removeAll(new_reduction);
                            if (!new_reduction.isEmpty()) {
                                curr.reduction.push(new_reduction);
                                cur.future_fc.add(curr);
                            }
                        }
                        if (curr.getCurrent_domain().isEmpty()) {
                            Constraint bad_apple = findConflictReason(cur,curr,X);
                            bad_apple.wdeg++;
                            findWipeout = true;
                            // undo-reductions
                            for (simpleVariable m : cur.future_fc) {
                                Set<Integer> reduction = m.reduction.pop();
                                m.getCurrent_domain().addAll(reduction);
                            }
                            cur.future_fc.clear();
                        } else {
                            findWipeout = false;
                        }
                    }
                    if (findWipeout) cur.getCurrent_domain().removeFirst();
                    if (!findWipeout) break;
                }
                if (cur.getCurrent_domain().isEmpty()) consistent = false;
                else consistent = true;
                if (consistent) {
                    ith++;
                }
            } else {
                //fc-unlabel
                bt++;
                int h = ith - 1;
                if (h == -1) break;
                // undo-reductions
                for (simpleVariable m : chooser.get(h).future_fc) {
                    Set<Integer> reduction = m.reduction.pop();
                    m.getCurrent_domain().addAll(reduction);
                }
                chooser.get(h).future_fc.clear();

                //update-current-domain
                cur.restore();
                for (Set<Integer> m : cur.reduction) {
                    cur.getCurrent_domain().removeAll(m);
                }

                chooser.get(h).getCurrent_domain().remove(Integer.valueOf(v[h]));
                consistent = !chooser.get(h).getCurrent_domain().isEmpty();
                ith = h;

                chooser.back();
            }
            if (ith == var_num) {
                if (search_solutions.isEmpty()) {
                    firstBt = bt;
                    firstCc = check_counts;
                    firstNv = nv;
                    firstTime = System.nanoTime();
                }
                search_solutions.add(chooser.transSequence(v));

/*                for(int i=0;i<ith;i++){
                    for(int j=0;j<i;j++){
                        assert(check(chooser.get(i),v[i],chooser.get(j),v[j]));
                    }
                }*/

                chooser.get(ith - 1).getCurrent_domain().remove(Integer.valueOf(v[ith - 1]));
                consistent = !chooser.get(ith - 1).getCurrent_domain().isEmpty();
                ith--;
                chooser.back();

                if (oneSolution) break;
            }
        }
    }

    // TODO Check if necessary
    static public boolean constraintInSimpleVariableList(Constraint c, List<simpleVariable> li){
        List<Variable> list = li.stream().map(simpleVariable::getRefVar).collect(Collectors.toList());
        return list.contains(c.scope[0]) && list.contains(c.scope[1]);
    }

    public boolean MACrevise(final simpleVariable a, final simpleVariable b,List<simpleVariable>unused) throws NoSolutionException {
        List<Boolean> rst_flag = a.getCurrent_domain().stream().map(va -> support(a, va, b)).collect(Collectors.toList());
        int x=a.getCurrent_domain().getLast();
        List<Integer> cut_counts = null;
        try{
            cut_counts = MACcutDomainByList(a.getCurrent_domain(), rst_flag);
        }
        catch (NoSolutionException e){
            Constraint bad_apple=findConflictReason(a,b,x);
            bad_apple.wdeg++;
            throw e;
        }
        a.reductionMAC.peek().addAll(cut_counts);
        return cut_counts.size() > 0;
    }

    public List<Integer> MACcutDomainByList(LinkedList<Integer> target, List<Boolean> flag_list) throws NoSolutionException {
        List<Integer> ret = new ArrayList<>();
        Iterator<Integer> it = target.iterator();
        assert target.size() == flag_list.size();
        if (!flag_list.contains(true)) {
            throw new NoSolutionException();
        }
        for (Boolean is_supported : flag_list) {
            Integer x = it.next();
            if (!is_supported) {
                ret.add(x);
                it.remove();
                remove_counts++;
            }
        }

        return ret;
    }

    private Stream<solverSimpleVarPair> MACgetInitQueue_stream(List<simpleVariable> unused) {
        return constraints.stream().filter(i -> i.getArity() == 2)
                .flatMap((i) -> {
                    //Double the pairs
                    simpleVariable a = findSimpleVariable.get(i.scope[0]);
                    simpleVariable b = findSimpleVariable.get(i.scope[1]);
                    if (!unused.contains(a) || !unused.contains(b)) {
                        return Stream.of();
                    }
                    return Stream.of(new solverSimpleVarPair(a, b), new solverSimpleVarPair(b, a));
                }).distinct();
    }

    private Queue<solverSimpleVarPair> MACgetInitQueue_queue(List<simpleVariable> unused) {
        Queue<solverSimpleVarPair> q = new ConcurrentLinkedQueue<>();
        MACgetInitQueue_stream(unused).forEach(q::offer);
        return q;
    }

    private void MAC(heuristicType mode) {
        boolean oneSolution = true;
        if(oneSolution) System.out.println("ONE SOLUTION: ON");
        if (mode.name().startsWith("d")) {
            variables.forEach(simpleVariable::initFC);
            chooser = new dynamicVariableChooser(variables, mode, isRelated);
        } else {
            chooser = new staticVariableChooser(variables, mode, isRelated);
        }
        int var_num = chooser.getSize();
        int v[] = new int[var_num];

        int ith = 0;

        boolean consistent = true;
        while (ith > -1) {
//            System.out.println(ith);
            if (ith > chooser.getCurSize()) chooser.next();
            simpleVariable cur = chooser.get(ith);

            if (consistent) {
                assert chooser.get(ith).future_fc.isEmpty();
                //fc-label
                assert (!cur.getCurrent_domain().isEmpty());
                while (!cur.getCurrent_domain().isEmpty()) {
                    v[ith] = cur.getCurrent_domain().getFirst();
                    nv++;
                    boolean findWipeout = false;

                    List<simpleVariable> unused = chooser.getUnusedVariables(ith);
                    unused.add(cur);

                    cur.pretendOne();
                    // AC3 MAC-Special
                    for (simpleVariable i : unused) {
                        i.reductionMAC.push(new ArrayList<>());
                    }
                    Queue<solverSimpleVarPair> q = MACgetInitQueue_queue(unused);
                    for (solverSimpleVarPair i : q) {
                        for (int j = 0; j <= ith; j++) {
                            assert i.getA() != chooser.get(j) && i.getB() != chooser.get(j);
                        }
                    }
                    while (q.size() != 0) {
                        boolean isRevised = false;
                        solverSimpleVarPair i = q.poll();
                        try {
                            isRevised = MACrevise(i.getA(), i.getB(),unused);
                        } catch (NoSolutionException e) {
                            findWipeout = true;
                            break;
                        }
                        if (isRevised) {
                            i.getA().getRefVar().constraints.parallelStream()
                                    .filter(j -> j.getArity() == 2)
                                    .flatMap(j -> Arrays.stream(j.scope))
                                    .filter(j -> !j.getName().equals(i.getA().getName()) && !j.getName().equals(i.getB().getName()))
                                    .map(j -> findSimpleVariable.get(j))
                                    .filter(j -> unused.contains(j))
                                    .distinct()
                                    .forEach((j) -> {
                                        q.offer(new solverSimpleVarPair(j, i.getA()));
                                    });
                        }
                    }
                    if (findWipeout) {
                        // undo-reductions-plus
                        for (simpleVariable i : unused) {
                            List<Integer> reductionMAC = i.reductionMAC.pop();
                            i.getCurrent_domain().addAll(reductionMAC);
                        }
                        // End
                    }
                    // End

                    cur.pretendBack();


                    if (findWipeout) cur.getCurrent_domain().removeFirst();
                    if (!findWipeout) break;
                }
                if (cur.getCurrent_domain().isEmpty()) consistent = false;
                else consistent = true;
                if (consistent) {
                    ith++;
                }
            } else {
                //fc-unlabel
                bt++;
                int h = ith - 1;
                if (h == -1) break;
                chooser.back();
                // undo-reductions-plus
                for (simpleVariable i : chooser.getUnusedVariables(h)) {
                    List<Integer> reductionMAC = i.reductionMAC.pop();
                    i.getCurrent_domain().addAll(reductionMAC);
                }

                // End

                //update-current-domain-plus
                cur.restore();
                for (List<Integer> m : cur.reductionMAC) {
                    cur.getCurrent_domain().removeAll(m);
                }

                chooser.get(h).getCurrent_domain().remove(Integer.valueOf(v[h]));
                consistent = !chooser.get(h).getCurrent_domain().isEmpty();
                ith = h;
            }
            if (ith == var_num) {
                if (search_solutions.isEmpty()) {
                    firstBt = bt;
                    firstCc = check_counts;
                    firstNv = nv;
                    firstTime = System.nanoTime();
                }

                search_solutions.add(chooser.transSequence(v));

/*                for(int i=0;i<ith;i++){
                    for(int j=0;j<i;j++){
                        assert(check(chooser.get(i),v[i],chooser.get(j),v[j]));
                    }
                }*/

                chooser.get(ith - 1).getCurrent_domain().remove(Integer.valueOf(v[ith - 1]));
                consistent = !chooser.get(ith - 1).getCurrent_domain().isEmpty();
                ith--;
                chooser.back();

                if (oneSolution) break;
            }
        }
//        constraints.forEach(i->System.out.println(i.wdeg));
    }
}


