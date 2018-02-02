package csp.tool;

/*import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;*/
import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import csp.MyParser;
import csp.data.Constraint;
import csp.data.simpleVariable;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

public class Solver {
    private csp.data.Problem problem = null;
    private int time_setup = -1;
    private int time_working = -1;
    private int check_counts = 0; //cc
    private long cpu_time=0;
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
        double ans= variables.parallelStream().map(i->(double)(i.getCurrent_domain().size())/i.getRefVar().getInitial_domain().length)
                .reduce((i,j)->i*j).get();
        return 1-ans;
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

    public boolean check(final simpleVariable a, int va, final simpleVariable b, int vb) {
        check_counts++;
        Optional<Boolean> flag = constraints.stream().filter(i -> i.getArity() == 2).map((i) -> {
            simpleVariable x = a, y = b;
            int vva=va,vvb=vb;
            if (i.scope[1].getName().equals(x.getName()) && i.scope[0].getName().equals(y.getName())) {
                simpleVariable c = x;
                x = y;
                y = c;
                int t=vva;
                vva=vvb;
                vvb=t;

            }
            if (i.scope[0].getName().equals(x.getName()) && i.scope[1].getName().equals(y.getName())) {
                int[] tuple = {vva, vvb};
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

    public boolean revise(final simpleVariable a, final simpleVariable b) throws NoSolutionException {
        List<Boolean> rst_flag = a.getCurrent_domain().stream().map(va -> support(a, va, b)).collect(Collectors.toList());
        int cut_counts = cutDomainByList(a.getCurrent_domain(), rst_flag);
        return cut_counts > 0;
    }

    public void AC1() {
        ArrayList<simpleVariable> q1=new ArrayList<simpleVariable>();
        ArrayList<simpleVariable> q2=new ArrayList<simpleVariable>();

        constraints.forEach(i->{
            if(i.getArity()!=2) return;
            simpleVariable a=variables.stream()
                    .filter(j->j.getName().equals(i.scope[0].getName()))
                    .findAny().get();
            simpleVariable b=variables.stream().filter(j->j.getName().equals(i.scope[1].getName())).findAny().get();
            q1.add(a);
            q2.add(b);
            q1.add(b);
            q2.add(a);
        });

        long startTime = System.currentTimeMillis();
        try {
            if (problem == null) {
                return;
            }
            boolean revised = true;

            while (revised) {
                revised = false;
                for(int k=0;k<q1.size();k++){
                    simpleVariable i=q1.get(k);
                    simpleVariable j=q2.get(k);
                    boolean x = revise(i, j);
                    revised = x || revised;
                }
            }
            finishedFlag = true;
            cpu_time=(System.currentTimeMillis() - startTime);
            System.out.println("Instance name: " + problem.name);
            System.out.println("cc: " + check_counts);
            System.out.println("cpu: " + cpu_time + "ms");
            System.out.println("fval: " + remove_counts);
            System.out.format("iSize: %.5f\n",getInitial_size());
            System.out.format("fSize: %.5f\n",getFiltered_size());
            System.out.println("fEffect: " + filterEffect());

        } catch (NoSolutionException e) {
            System.out.println("Instance name: " + problem.name);
            System.out.println("cc: " + check_counts);
            System.out.println("cpu: " + (System.currentTimeMillis() - startTime) + " ms");
            System.out.println("fval: " + remove_counts +" (before discovering domain wipeout)");
            System.out.format("iSize: %.5f\n",getInitial_size());
            System.out.println("fSize: " + "false");
            System.out.println("fEffect: " + "false");
            try {
                Writer writer = new FileWriter("solver_output.csv",true);
                StatefulBeanToCsv beanToCsv = new StatefulBeanToCsvBuilder(writer)
                        .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                        .build();
                solverReporter rpt = new solverReporter(MyParser.file_name,problem.name, check_counts, cpu_time, remove_counts, getInitial_size(), -1, -1);

                beanToCsv.write(rpt);
                writer.close();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ee) {
                    ee.printStackTrace();
                }
            } catch (CsvDataTypeMismatchException ee) {
                ee.printStackTrace();
            } catch (CsvRequiredFieldEmptyException ee) {
                ee.printStackTrace();
            } catch (IOException ee) {
                ee.printStackTrace();
            }
            return ;
        }
        try {
            Writer writer = new FileWriter("solver_output.csv",true);
            StatefulBeanToCsv beanToCsv = new StatefulBeanToCsvBuilder(writer)
                    .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                    .build();
            solverReporter rpt = new solverReporter(MyParser.file_name,problem.name, check_counts, cpu_time, remove_counts, getInitial_size(), getFiltered_size(), filterEffect());

            beanToCsv.write(rpt);
            writer.close();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (CsvDataTypeMismatchException e) {
            e.printStackTrace();
        } catch (CsvRequiredFieldEmptyException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void NC() {

        if (problem == null) {
            System.out.println("ERROR: NOT INITIALIZED");
            return;
        }
        variables.stream().forEach((i) -> {
            List<Boolean> rst_flag = i.getCurrent_domain().stream().map(j -> check(i, j)).collect(Collectors.toList());
            try {
                cutDomainByList(i.getCurrent_domain(), rst_flag);
            } catch (NoSolutionException e) {
                System.out.println("NO SOLUTION");
            }
        });
    }

}


