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

    public enum SOLUTIONS_bt {BT, CBJ, FC, FCCBJ}

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

        if (x == SOLUTIONS_bt.BT) {
            BT(var_heuristic);
        }else if (x == SOLUTIONS_bt.CBJ){
            CBJ(var_heuristic);
        }else if(x== SOLUTIONS_bt.FC){
            FC(var_heuristic);
        }else if(x==SOLUTIONS_bt.FCCBJ){
            FCCBJ(var_heuristic);
        }
        else {
            System.out.println("ERROR: Solution not exist");
        }

        finishedFlag = true;
        long first_cpu_time = (firstTime - startTime) / 1000000;
        long cpu_time = (System.nanoTime() - startTime) / 1000000;
        if(search_solutions.size()==0){
            return new solverReporter_bt(chooser, problem.name, problem.BTtype,
                    var_heuristic.toString(), var_heuristic.name().startsWith("d")?"dynamic": "static",
                    "LX", "static",
                    firstCc, firstNv, firstBt, -1, new ArrayList<Integer>(),
                    check_counts, nv, bt, cpu_time, 0);
        }
        return new solverReporter_bt(chooser, problem.name, problem.BTtype,
                var_heuristic.toString(), var_heuristic.name().startsWith("d")?"dynamic": "static",
                "LX", "static",
                firstCc, firstNv, firstBt, first_cpu_time, search_solutions.get(0),
                check_counts, nv, bt, cpu_time, search_solutions.size());
    }
    private relatedJudge isRelated = null;
    private variableChooser chooser;

    private void BT(heuristicType mode){
        chooser = new staticVariableChooser(variables, mode, isRelated);
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

                chooser.get(ith-1).getCurrent_domain().remove(Integer.valueOf(v[ith-1]));
                consistent = !chooser.get(ith-1).getCurrent_domain().isEmpty();
                ith--;
            }
        }
    }

    private void CBJ(heuristicType mode){
        chooser = new staticVariableChooser(variables, mode, isRelated);
        int var_num = chooser.getSize();
        int v[] = new int[var_num];
        Set<Integer> conf_set[] = new TreeSet[var_num];
        for(int i=0;i<conf_set.length;i++){
            conf_set[i]=new TreeSet<>();
            conf_set[i].add(-1);
        }
        boolean consistent = true;
        int ith = 0;
        //search
        while (ith >=0) {
//            System.out.println(ith);
//            if(ith==0) System.out.println("XX");
            simpleVariable cur = chooser.get(ith);
            if (consistent) {
                //bt-label
                assert(!cur.getCurrent_domain().isEmpty());
                while (!cur.getCurrent_domain().isEmpty()) {
                    v[ith] = cur.getCurrent_domain().getFirst();
                    assert(check(cur,v[ith],chooser.get(0),v[0]));

                    nv++;
                    boolean findConflict=false;
                    int k;
                    for (k = 0; !findConflict && k < ith; k++) {
                        if (isRelated.isExist(cur,chooser.get(k))) {
                            findConflict = !check(cur, v[ith], chooser.get(k), v[k]);
                        }
                    }
                    if (findConflict){
                        conf_set[ith].add(k-1);
                        cur.getCurrent_domain().removeFirst();
                    }
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
                int h = conf_set[ith].stream().max(Integer::compareTo).get();
                if(h<0) break;

                conf_set[h].addAll(conf_set[ith]);
                conf_set[h].remove(Integer.valueOf(h));
                for(int j=h+1;j<=ith;j++){
                    conf_set[j].clear();
                    conf_set[j].add(-1);
                    chooser.get(j).restore();
                }
                chooser.get(h).getCurrent_domain().remove(Integer.valueOf(v[h]));
                consistent = !chooser.get(h).getCurrent_domain().isEmpty();
                ith = h;
            }
            if (ith >= var_num) {
                if (search_solutions.isEmpty()) {
                    firstBt = bt;
                    firstCc = check_counts;
                    firstNv = nv;
                    firstTime = System.nanoTime();
                }
                search_solutions.add(chooser.transSequence(v));

                for(int i=0;i<ith-1;i++) conf_set[ith-1].add(i);

                chooser.get(ith-1).getCurrent_domain().remove(Integer.valueOf(v[ith-1]));
                consistent = !chooser.get(ith-1).getCurrent_domain().isEmpty();
                ith--;
            }
        }
    }

    private void FC(heuristicType mode){
        boolean oneSolution=true;
        if(mode.name().startsWith("d")){
            variables.forEach(simpleVariable::initFC);
            chooser = new dynamicVariableChooser(variables, mode, isRelated);
        }
        else{
            chooser = new staticVariableChooser(variables, mode, isRelated);
        }
        int var_num = chooser.getSize();
        int v[] = new int[var_num];

        int ith=0;

        boolean consistent=true;
        while (ith > -1) {
//            System.out.println(ith);
            if(ith>chooser.getCurSize()) chooser.next();
            simpleVariable cur = chooser.get(ith);

            if (consistent) {
                assert chooser.get(ith).future_fc.isEmpty();
                //fc-label
                assert(!cur.getCurrent_domain().isEmpty());
                while (!cur.getCurrent_domain().isEmpty()) {
                    v[ith] = cur.getCurrent_domain().getFirst();
                    nv++;
                    boolean findWipeout=false;
                    List<simpleVariable> unused=chooser.getUnusedVariables(ith);
                    for (int k = 0; !findWipeout && k < unused.size(); k++) {
                        simpleVariable curr= unused.get(k);
                        if (isRelated.isExist(cur,curr)) {
                            //check-foward
                            Set<Integer> new_reduction=new TreeSet<>();
                            for(int m:curr.getCurrent_domain()){
                                if(!check(cur, v[ith], curr, m)){
                                    new_reduction.add(m);
                                }
                            }
                            curr.getCurrent_domain().removeAll(new_reduction);
                            if(!new_reduction.isEmpty()){
                                curr.reduction.push(new_reduction);
                                cur.future_fc.add(curr);
                            }
                        }
                        if(curr.getCurrent_domain().isEmpty()){
                            findWipeout=true;
                            // undo-reductions
                            for(simpleVariable m:cur.future_fc){
                                Set<Integer> reduction = m.reduction.pop();
                                m.getCurrent_domain().addAll(reduction);
                            }
                            cur.future_fc.clear();
                        }
                        else{
                            findWipeout=false;
                        }
                    }
                    if (findWipeout) cur.getCurrent_domain().removeFirst();
                    if (!findWipeout) break;
                }
                if(cur.getCurrent_domain().isEmpty()) consistent=false;
                else consistent=true;
                if (consistent) {
                    ith++;
                }
            } else {
                //fc-unlabel
                bt++;
                int h = ith - 1;
                if(h==-1) break;
                // undo-reductions
                for(simpleVariable m:chooser.get(h).future_fc){
                    Set<Integer> reduction = m.reduction.pop();
                    m.getCurrent_domain().addAll(reduction);
                }
                chooser.get(h).future_fc.clear();

                //update-current-domain
                cur.restore();
                for(Set<Integer> m:cur.reduction){
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

                chooser.get(ith-1).getCurrent_domain().remove(Integer.valueOf(v[ith-1]));
                consistent = !chooser.get(ith-1).getCurrent_domain().isEmpty();
                ith--;
                chooser.back();

                if(oneSolution) break;
            }
        }
    }

    private void FCCBJ(heuristicType mode){
        boolean oneSolution=false;
        if(mode.name().startsWith("d")){
            variables.forEach(simpleVariable::initFC);
            chooser = new dynamicVariableChooser(variables, mode, isRelated);
        }
        else{
            chooser = new staticVariableChooser(variables, mode, isRelated);
        }
        int var_num = chooser.getSize();
        int v[] = new int[var_num];

        int ith=0;

        boolean consistent=true;
        while (ith > -1) {
//            System.out.println(ith);
            if(ith>chooser.getCurSize()) chooser.next();
            simpleVariable cur = chooser.get(ith);

            if (consistent) {
                assert chooser.get(ith).future_fc.isEmpty();
                //fc-cbj-label
                assert(!cur.getCurrent_domain().isEmpty());
                while (!cur.getCurrent_domain().isEmpty()) {
                    v[ith] = cur.getCurrent_domain().getFirst();
                    nv++;
                    boolean findWipeout=false;
                    List<simpleVariable> unused=chooser.getUnusedVariables(ith);
                    for (int k = 0; !findWipeout && k < unused.size(); k++) {
                        simpleVariable curr= unused.get(k);
                        if (isRelated.isExist(cur,curr)) {
                            //check-foward
                            Set<Integer> new_reduction=new TreeSet<>();
                            for(int m:curr.getCurrent_domain()){
                                if(!check(cur, v[ith], curr, m)){
                                    new_reduction.add(m);
                                }
                            }
                            curr.getCurrent_domain().removeAll(new_reduction);
                            if(!new_reduction.isEmpty()){
                                curr.reduction.push(new_reduction);
                                cur.future_fc.add(curr);
                                curr.past_fc.add(cur);
                            }
                        }
                        if(curr.getCurrent_domain().isEmpty()){
                            findWipeout=true;

                            // undo-reductions Start
                            for(simpleVariable m:cur.future_fc){
                                Set<Integer> reduction = m.reduction.pop();
                                m.getCurrent_domain().addAll(reduction);
                                m.past_fc.removeLast();
                            }
                            cur.future_fc.clear();
                            // undo-reductions End

                            cur.conf_set.addAll(curr.past_fc);
                            assert !cur.conf_set.contains(cur);
                        }
                        else{
                            findWipeout=false;
                        }
                    }
                    if (findWipeout) cur.getCurrent_domain().removeFirst();
                    if (!findWipeout) break;
                }
                if(cur.getCurrent_domain().isEmpty()) consistent=false;
                else consistent=true;
                if (consistent) {
                    ith++;
                }
            } else {
                //fc-cbj-unlabel
                bt++;
                int h = ith - 1;
                while(h>=0&&!cur.conf_set.contains(chooser.get(h))&&!cur.past_fc.contains(chooser.get(h))){
                    h--;
                }
                if(h==-1) break;

                simpleVariable H=chooser.get(h);
                H.conf_set.addAll(cur.conf_set);
                H.conf_set.addAll(cur.past_fc);
                H.conf_set.remove(H);
                assert !H.conf_set.contains(H);

                for(int j=ith; j>=h+1; j--){
                    simpleVariable curr=chooser.get(j);

                    curr.conf_set.clear();

                    // undo-reductions(J) Start
                    for(simpleVariable m:curr.future_fc){
                        Set<Integer> reduction = m.reduction.pop();
                        m.getCurrent_domain().addAll(reduction);
                        m.past_fc.removeLast();
                    }
                    curr.future_fc.clear();
                    // End

                    // update-current-domain J
                    curr.restore();
                    for(Set<Integer> m:curr.reduction){
                        curr.getCurrent_domain().removeAll(m);
                    }
                    // End

                    chooser.back();
                }

                // undo-reductions(H)
                for(simpleVariable m:H.future_fc){
                    Set<Integer> reduction = m.reduction.pop();
                    m.getCurrent_domain().addAll(reduction);
                    m.past_fc.removeLast();
                }
                H.future_fc.clear();
                // undo end

                H.getCurrent_domain().remove(Integer.valueOf(v[h]));
                consistent = !H.getCurrent_domain().isEmpty();
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
                for(int i=0;i<ith-1;i++) chooser.get(ith-1).conf_set.add(chooser.get(i));
                chooser.get(ith-1).getCurrent_domain().remove(Integer.valueOf(v[ith-1]));
                consistent = !chooser.get(ith-1).getCurrent_domain().isEmpty();
                ith--;
                chooser.back();

                if(oneSolution) break;
            }
        }
    }
}


