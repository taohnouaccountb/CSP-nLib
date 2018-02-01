package csp.tool;

import csp.data.Constraint;
import csp.data.simpleVariable;

import java.util.*;
import java.util.stream.Collectors;

public class Solver {
    private csp.data.Problem problem = null;
    private int time_setup = -1;
    private int time_working = -1;
    private int check_counts = 0; //cc
    private int remove_counts = 0; //fval

    private Boolean finishedFlag = false;

    private long initial_size = -1;

    public Solver() {
    }

    public long getInitial_size() {
        if (problem == null) System.out.println("ERROR: Solver has not been initialized.");
        if (initial_size == -1) {
            initial_size = calc_size();
            return initial_size;
        } else {
            return initial_size;
        }
    }

    public long getFiltered_size() {
        return calc_size();
    }

    private long calc_size() {
        boolean flag = false;
        long ans = 1;
        for (simpleVariable i : variables) {
            if (ans > 1e15) {
                flag = true;
                break;
            }
            ans *= i.getCurrent_domain().size();
        }
        double dans = 0;
        for (simpleVariable i : variables) {
            dans += java.lang.Math.log10((double) i.getCurrent_domain().size());
        }
        return (long) -dans;
    }

    public double filterEffect() {
        return -1;
    }

    private List<simpleVariable> variables;
    private List<Constraint> constraints;

    public void init(csp.data.Problem problem) {
        this.problem = problem;
        long startTime = System.currentTimeMillis();

        variables = new ArrayList<simpleVariable>();
        /*threadFactory.submit(()->{
           problem.variables.forEach(i-> {
               variables.add(new simpleVariable(i));
//               System.out.println("VARIABLE "+i.getName());
           });
        });*/
        problem.variables.forEach(i -> {
            variables.add(new simpleVariable(i));
        });

        constraints = new ArrayList<Constraint>();
        problem.constraints.forEach(i -> {
            constraints.add(i);
        });


//        System.out.println("VAR_SIZE: "+variables.size());
//        System.out.println("CST_SIZE: "+constraints.size());

        long endTime = System.currentTimeMillis();
//        System.out.println("INIT_TIME: " + (endTime - startTime)+"ms");
        getInitial_size();

    }

    public int cutDomainByList(LinkedList<Integer> target, List<Boolean> flag_list) {
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
        return count;
    }

    public boolean check(final simpleVariable a, final int va, final simpleVariable b, final int vb) {
        check_counts++;
        Optional<Boolean> flag = constraints.parallelStream().filter(i -> i.getArity() == 2).map((i) -> {
            simpleVariable x = a, y = b;
            if (i.scope[1].getName().equals(x.getName()) && i.scope[0].getName().equals(y.getName())) {
                simpleVariable c = x;
                x = y;
                y = c;
            }
            if (i.scope[0].getName().equals(x.getName()) && i.scope[1].getName().equals(y.getName())) {
                int[] tuple = {va, vb};
                try {
                    return i.check(tuple);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        }).filter(i -> i == false).findAny();
        if (flag.isPresent()) {
            return false;
        }
        return true;
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

    public boolean support(final simpleVariable a, final int va, final simpleVariable b) {
        return b.getCurrent_domain().stream().map(i -> check(a, va, b, i)).filter(i -> i == true).findAny().isPresent();
    }

    public boolean revise(final simpleVariable a, final simpleVariable b) {
        boolean revised = false;
        List<Boolean> rst_flag = a.getCurrent_domain().stream().map(va -> support(a, va, b)).collect(Collectors.toList());
        int cut_counts = cutDomainByList(a.getCurrent_domain(), rst_flag);
        return cut_counts > 0;
    }

    public void AC1() {
        if (problem == null) {
            return;
        }
        long startTime = System.currentTimeMillis();
        boolean revised = true;
        while (revised) {
            revised = false;
            for (simpleVariable i : variables) {
                for (simpleVariable j : variables) {
                    if (i == j) continue;
                    boolean x = revise(i, j);
                    revised = x || revised;
                }
            }
        }
        finishedFlag = true;
        System.out.println("Instance name: " + problem.name);
        System.out.println("cc: " + check_counts);
        System.out.println("cpu: " + (System.currentTimeMillis() - startTime) + "ms");
        System.out.println("fval: " + remove_counts);
        System.out.println("iSize: " + getInitial_size());
        System.out.println("fSize: " + getFiltered_size());
        System.out.println("fEffect: " + filterEffect());
    }

    public void NC() {
        if (problem == null) {
            return;
        }
        variables.stream().map(i -> {
            List<Boolean> rst_flag = i.getCurrent_domain().stream().map(j -> check(i, j)).collect(Collectors.toList());
            cutDomainByList(i.getCurrent_domain(), rst_flag);
            return 1;
        });
    }

}
