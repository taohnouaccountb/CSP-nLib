package csp.tool;

import csp.data.simpleVariable;
import csp.tool.bt.PartialAC3;
import csp.tool.bt.dynamicVariableChooser;
import csp.tool.bt.variableChooser;

import java.util.*;
import java.util.stream.Stream;

public class Solver_MAC extends Solver {
    private boolean oneSolution = true;

    private relatedJudge isRelated;
    private dynamicVariableChooser chooser;
    private int var_num;
    private int v[];
    private int ith;

    private void init(variableChooser.heuristicType var_heuristic){
        if(oneSolution) System.out.println("ONE SOLUTION: ON");
        isRelated = new relatedJudge();

        chooser = new dynamicVariableChooser(variables, var_heuristic, isRelated);

        var_num = chooser.getSize();
        v = new int[var_num];

        variables.forEach(simpleVariable::initFC);
        search_solutions = new ArrayList<>();
        remove_counts = 0;
        check_counts = 0;
        firstTime = 0;
        nv = 0;
        bt = 0;

        ith = 0;
    }

    public solverReporter_bt solve_bt(SOLUTIONS_bt x, variableChooser.heuristicType var_heuristic) {
        init(var_heuristic);

        if (x != SOLUTIONS_bt.MAC) throw new UnknownError("Other BT-Algo not supported here.");
        if (problem == null) throw new UnknownError("Problem not initialized.");

        long startTime = System.nanoTime();
        MAC();
        return reportBT(startTime, var_heuristic);
    }

    private void MAC() {
        boolean consistent = true;
        while (ith > -1) {
            if (ith > chooser.getCurSize()) chooser.next();
            simpleVariable cur = chooser.get(ith);

            if (consistent) {
                if(!chooser.get(ith).future_fc.isEmpty()) throw new UnknownError();
                if(cur.getCurrent_domain().isEmpty()){
                    System.out.println(ith);
                    throw new UnknownError();
                }

                //fc-label
                while (!cur.getCurrent_domain().isEmpty()) {
                    v[ith] = cur.getCurrent_domain().getFirst();
                    nv++;

                    //Add current variable to the set
                    List<simpleVariable> unusedWithCur = chooser.getUnusedVariables(ith);
                    unusedWithCur.add(cur);

                    cur.pretendOne();

                    boolean findWipeout = (new PartialAC3(unusedWithCur)).pAC3();

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
                solution_saver(chooser, v);
                if (oneSolution) break;

//                chooser.get(ith - 1).getCurrent_domain().remove(Integer.valueOf(v[ith - 1]));
                chooser.get(ith - 1).getCurrent_domain().removeFirst();
                consistent = !chooser.get(ith - 1).getCurrent_domain().isEmpty();

                ith--;
            }
        }
    }

    public static Stream<solverSimpleVarPair> getInitQueue_stream(List<simpleVariable> target_list){
        return constraints.stream().filter(i -> i.getArity() == 2)
                .flatMap((i) -> {
                    //Double the pairs
                    simpleVariable a = findSimpleVariable.get(i.scope[0]);
                    simpleVariable b = findSimpleVariable.get(i.scope[1]);
                    if (target_list.contains(a) && target_list.contains(b)) {
                        return Stream.of(new solverSimpleVarPair(a, b), new solverSimpleVarPair(b, a));
                    }
                    else{
                        return Stream.of();
                    }
                });
    }
}
