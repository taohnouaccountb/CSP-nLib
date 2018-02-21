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
import csp.tool.bt.variableChooser;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
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

    public Solver() {
    }

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
        //TODO utilize performance
        Boolean haveConflict = constraints.stream().filter(i -> i.getArity() == 2).anyMatch((i) -> {
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
        return constraints.parallelStream().filter(i -> i.getArity() == 2)
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

    private void AC3() throws NoSolutionException {
        Queue<solverSimpleVarPair> q = getInitQueue_queue();
        while (q.size() != 0) {
            boolean isRevised = false;
            solverSimpleVarPair i = q.poll();
            isRevised = revise(i.getA(), i.getB());
            if (isRevised) {
                i.getA().getRefVar().constraints.parallelStream()
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

    private void AC2001() throws NoSolutionException {
        List<solverSimpleVarPair> l = getInitQueue_list();
        ac2001Track = new HashMap<>();
        for (solverSimpleVarPair i : l) {
            ac2001Track.putIfAbsent(i.getA(), new HashMap<>());
            ac2001Track.get(i.getA()).putIfAbsent(i.getB(), new HashMap<>());
        }

        ac2001_enable = true;
        AC3();
        ac2001_enable = false;

    }

    private void AC1() throws NoSolutionException {
        List<solverSimpleVarPair> q = getInitQueue_list();
        boolean revised = true;
        while (revised) {
            revised = false;
            for (int k = 0; k < q.size(); k++) {
                solverSimpleVarPair i = q.get(k);
                boolean x = revise(i.getA(), i.getB());
                revised = x || revised;
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

    public void AC_trim(){
        variables.forEach(simpleVariable::reverseRestore);
    }

    private long firstTime = 0;
    private int firstCc = 0;
    private int firstNv = 0;
    private int firstBt = 0;
    private int nv = 0;
    private int bt = 0;

    public enum SOLUTIONS_bt {BT}

    private List<List<Integer>> bt_solutions = null;
    public solverReporter_bt solve_bt(SOLUTIONS_bt x, variableChooser.heuristicType var_heuristic) {
        bt_solutions = new ArrayList<>();
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

        if (x == SOLUTIONS_bt.BT) {
            BT(var_heuristic);
        } else {
            System.out.println("ERROR: Solution not exist");
        }

        finishedFlag = true;
        long first_cpu_time = (firstTime - startTime) / 1000000;
        long cpu_time = (System.nanoTime() - startTime) / 1000000;
        if(bt_solutions.size()==0){
            return new solverReporter_bt(problem.name,
                    var_heuristic.toString(), "static",
                    "LX", "static",
                    firstCc, firstNv, firstBt, first_cpu_time, new ArrayList<Integer>(),
                    check_counts, nv, bt, cpu_time, 0);
        }
        return new solverReporter_bt(problem.name,
                var_heuristic.toString(), "static",
                "LX", "static",
                firstCc, firstNv, firstBt, first_cpu_time, bt_solutions.get(0),
                check_counts, nv, bt, cpu_time, bt_solutions.size());
    }


    relatedJudge isRelated = null;

    private void BT(variableChooser.heuristicType mode){
        variableChooser chooser = new variableChooser(variables, mode, isRelated);
        int var_num = chooser.getSize();
        int v[] = new int[var_num];

        boolean consistent = true;
        int ith = 0;
        //search
        while (ith > -1) {
            simpleVariable cur = chooser.get(ith);
            if (consistent) {
                //bt-label
                assert(!cur.getCurrent_domain().isEmpty());
                while (!cur.getCurrent_domain().isEmpty()) {
                    v[ith] = cur.getCurrent_domain().getFirst();
                    nv++;
                    boolean findConflict=false;
                    for (int k = 0; !findConflict && k < ith; k++) {
                        if (isRelated.isExist(cur,chooser.get(k))) {
                            findConflict = !check(cur, v[ith], chooser.get(k), v[k]);
                        }
                    }
                    if (findConflict) cur.getCurrent_domain().removeFirst();
                    if (!findConflict) break;
                }
                if(cur.getCurrent_domain().isEmpty()) consistent=false;
                else consistent=true;

                if (consistent) {
                    ith++;
                }
            } else {
                //bt-unlabel
                bt++;
                int h = ith - 1;
                if(h==-1) break;
                chooser.get(ith).restore();
                chooser.get(h).getCurrent_domain().remove(Integer.valueOf(v[h]));
                consistent = !chooser.get(h).getCurrent_domain().isEmpty();
                ith = h;
            }
            if (ith >= var_num) {
                if (bt_solutions.isEmpty()) {
                    firstBt = bt;
                    firstCc = check_counts;
                    firstNv = nv;
                    firstTime = System.nanoTime();
                }
                bt_solutions.add(chooser.transSequence(v));

/*                for(int i=0;i<ith;i++){
                    for(int j=0;j<i;j++){
                        assert(check(chooser.get(i),v[i],chooser.get(j),v[j]));
                    }
                }*/

                chooser.get(ith-1).getCurrent_domain().remove(Integer.valueOf(v[ith-1]));
                consistent = !chooser.get(ith-1).getCurrent_domain().isEmpty();
                ith--;
            }
        }
    }

}


