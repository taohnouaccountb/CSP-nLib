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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Solver {
    private double initial_size = -1;
    protected csp.data.Problem problem = null;


    private relatedJudge isRelated = null;
    private variableChooser chooser = null;

    // Record variables
    protected long firstTime = 0;
    protected int firstCc = 0;
    protected int firstNv = 0;
    protected int firstBt = 0;
    public static int check_counts = 0; //cc
    static protected int nv = 0;
    static protected int bt = 0;
    protected List<List<Integer>> search_solutions = null;
    protected int remove_counts = 0; //fval

    protected List<simpleVariable> variables = null;
    static public List<Constraint> constraints = null;
    static public Map<Variable, simpleVariable> findSimpleVariable = null;

    public enum SOLUTIONS_ac {AC3, NC}
    public enum SOLUTIONS_bt {FC, MAC}

    public Solver() {}

    // Tool functions
    protected solverReporter_bt reportBT(long startTime, variableChooser.heuristicType var_heuristic){
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

    protected void solution_saver(variableChooser chooser, int[] v){
        if (search_solutions.isEmpty()) {
            firstBt = bt;
            firstCc = check_counts;
            firstNv = nv;
            firstTime = System.nanoTime();
        }
        search_solutions.add(chooser.transSequence(v));
//        System.out.println(search_solutions.get(search_solutions.size()-1));
    }

    public static List<Constraint> constraintsBetween(simpleVariable a, simpleVariable b){
        return a.getRefVar().constraints.stream()
                .filter(i -> i.getArity() == 2)
                .filter(i->i.scopeContainsSimpleVariable(b))
                .collect(Collectors.toList());
    }

    public void init(csp.data.Problem problem) {
        this.problem = problem;

        findSimpleVariable = new HashMap<>();
        variables = new ArrayList<>();
        problem.variables.forEach(i -> {
            simpleVariable n = new simpleVariable(i);
            variables.add(n);
            findSimpleVariable.put(i, n);
        });

        constraints = new ArrayList<>();
        constraints.addAll(problem.constraints);

        getInitial_size();

    }

    private double getInitial_size() {
        if (problem == null) System.out.println("ERROR: Solver has not been initialized.");
        if (initial_size == -1) {
            initial_size = calc_size();
            return initial_size;
        } else {
            return initial_size;
        }
    }

    private double getFiltered_size() {
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

    private double filterEffect() {
        double ans = variables.parallelStream().map(i -> (double) (i.getCurrent_domain().size()) / i.getRefVar().getInitial_domain().length)
                .reduce((i, j) -> i * j).get();
        return 1 - ans;
    }

    private int cutDomainByList(LinkedList<Integer> target, List<Boolean> flag_list) throws NoSolutionException {
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

    private boolean check_old(simpleVariable a, int va, simpleVariable b, int vb) {
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

    private Constraint reasonOfLastCheckFailed;
    private boolean check(simpleVariable a, int va, simpleVariable b, int vb) {
        Solver.check_counts++;
        reasonOfLastCheckFailed=null;

        if(!a.getRefVar().neighbors.contains(b.getRefVar())) return true;

        List<Constraint> constraints = Solver.constraintsBetween(a,b);

        for(Constraint i:constraints){
            boolean haveOneConflictConstraint = false;
            if(a.equals_VAR(i.scope[0])){
                assert(b.equals_VAR(i.scope[1]));
                //scope = [a,b]
                int[] tuple = {va, vb};
                try {
                    haveOneConflictConstraint = !i.check(tuple);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else if(a.equals_VAR(i.scope[1])){
                assert(b.equals_VAR(i.scope[0]));
                //scope = [b,a]
                int[] tuple = {vb, va};
                try {
                    haveOneConflictConstraint = !i.check(tuple);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else{
                throw new UnknownError("SO BAD");
            }
            if(haveOneConflictConstraint) {
                reasonOfLastCheckFailed = i;
                return false;
            }
        }
        return true;
    }

    private boolean check(final simpleVariable a, final int va) {
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

    private boolean support(final simpleVariable a, final int va, final simpleVariable b) {
        return b.getCurrent_domain().stream().anyMatch(i -> check(a, va, b, i));
    }

    private boolean revise(final simpleVariable a, final simpleVariable b) throws NoSolutionException {
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



    public solverReporter_ac solve_ac(SOLUTIONS_ac x) {
        remove_counts = 0;
        long startTime = System.nanoTime();

        try {
            assert(problem!=null);
            if (x == SOLUTIONS_ac.AC3) {
                AC3();
            } else if (x == SOLUTIONS_ac.NC) {
                NC();
            } else {
                System.out.println("ERROR: Solution not exist");
            }
            long cpu_time = (System.nanoTime() - startTime) / 1000000;
            return new solverReporter_ac(MyParser.file_name, problem.name, check_counts, cpu_time, remove_counts,
                    getInitial_size(), getFiltered_size(), filterEffect());
        } catch (NoSolutionException e) {
            long cpu_time = 0;
            return new solverReporter_ac(MyParser.file_name, problem.name, check_counts, cpu_time, remove_counts,
                    getInitial_size(), -1, -1);
        }
    }

    private void AC3() throws NoSolutionException {
        Queue<solverSimpleVarPair> q = getInitQueue_queue();
        while (q.size() != 0) {
            boolean isRevised;
            solverSimpleVarPair i = q.poll();
            isRevised = revise(i.getA(), i.getB());
            if (isRevised) {
                i.getA().getRefVar().constraints.stream()
                        .filter(j -> j.getArity() == 2)
                        .flatMap(j -> Arrays.stream(j.scope))
                        .filter(j -> !j.getName().equals(i.getA().getName()) && !j.getName().equals(i.getB().getName()))
                        .map(j -> findSimpleVariable.get(j))
                        .distinct()
                        .forEach((j) -> {
                            q.offer(new solverSimpleVarPair(j, i.getA()));
                        });
            }
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



    public solverReporter_bt solve_bt(SOLUTIONS_bt x, heuristicType var_heuristic) {
        search_solutions = new ArrayList<>();
        remove_counts = 0;
        check_counts = 0;
        firstTime = 0;
        nv = 0;
        bt = 0;
        long startTime = System.nanoTime();

        isRelated = new relatedJudge();

        if (problem == null) {
            System.out.println("ERROR: Problem Uninitialized");
        }

        if (x == SOLUTIONS_bt.FC) {
            FC(var_heuristic);
        } else {
            System.out.println("ERROR: Solution not exist");
        }

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
                            reasonOfLastCheckFailed.wdeg++;
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

                chooser.get(ith - 1).getCurrent_domain().removeFirst();
//                chooser.get(ith - 1).getCurrent_domain().remove(Integer.valueOf(v[ith - 1]));
                consistent = !chooser.get(ith - 1).getCurrent_domain().isEmpty();
                ith--;
//                chooser.back();

                if (oneSolution) break;
            }
        }
    }

// TODO Delete if not necessary



}


