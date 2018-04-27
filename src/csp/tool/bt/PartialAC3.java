package csp.tool.bt;

import csp.data.Constraint;
import csp.data.simpleVariable;
import csp.tool.NoSolutionException;
import csp.tool.Solver;
import csp.tool.Solver_MAC;
import csp.tool.solverSimpleVarPair;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class PartialAC3 {
    private List<simpleVariable> target_list;

    public PartialAC3(List<simpleVariable> var_li){
        this.target_list = var_li;
    }

    private Constraint reasonOfLastCheckFailed;
    private boolean check(simpleVariable a, int va, simpleVariable b, int vb) {
        reasonOfLastCheckFailed=null;
        if(!a.getRefVar().neighbors.contains(b.getRefVar())) return true; // No relation, no check needed

        Solver.check_counts++;
        if (Solver.check_counts%10000==0) System.out.println(Solver.check_counts);
        List<Constraint> constraints = Solver.constraintsBetween(a,b);

        for(Constraint i:constraints){
            boolean haveOneConflictConstraint = false;
            if(a.equals_VAR(i.scope[0])){
                //scope = [a,b]
                int[] tuple = {va, vb};
                try {
                    haveOneConflictConstraint = !i.check(tuple);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else if(a.equals_VAR(i.scope[1])){
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

    private boolean support(final simpleVariable a, final int va, final simpleVariable b) {
        return b.getCurrent_domain().stream().anyMatch(i -> check(a, va, b, i));
    }

    private boolean revise(final simpleVariable a, final simpleVariable b) throws NoSolutionException {
        assert(a.getRefVar().neighbors.contains(b.getRefVar()));
        List<Boolean> rst_flag = a.getCurrent_domain().stream().map(va -> support(a, va, b)).collect(Collectors.toList());
        boolean needReduceDomain = rst_flag.contains(false);
        if(!rst_flag.contains(true)){
            //No solution
            reasonOfLastCheckFailed.wdeg++;
            throw new NoSolutionException();
        }

        // Start reducing
        List<Integer> eliminatedValues = cutDomainByListReturnFailures(a.getCurrent_domain(), rst_flag);
        a.reductionMAC.peek().addAll(eliminatedValues);
        return needReduceDomain;
    }

    private List<Integer> cutDomainByListReturnFailures(LinkedList<Integer> target, List<Boolean> flag_list){
        // return the List of eliminated values
        if (!flag_list.contains(true)) throw new UnknownError("WipeoutShouldBeCheckedBeforePassIn");

        int count = 0;
        List<Integer> ret = new ArrayList<>();
        Iterator<Integer> it = target.iterator();
        if(target.size() != flag_list.size()) throw new UnknownError();
        for (Boolean is_supported : flag_list) {
            Integer x = it.next();
            if (!is_supported) {
                ret.add(x);
                it.remove();
                count++;
            }
        }
        return ret;
    }

    private void undo_reductions_MAC(){
        // only used here
        for (simpleVariable i : target_list) {
            List<Integer> reductionMAC = i.reductionMAC.pop();
            i.getCurrent_domain().addAll(reductionMAC);
        }
    }

    public boolean pAC3(){
        // AC3 MAC-Special
        boolean findWipeout = false;

        target_list.forEach(i->i.reductionMAC.push(new ArrayList<>()));
        Queue<solverSimpleVarPair> q = getInitQueue_queue();
//        for (solverSimpleVarPair i : q) {
//            for (int j = 0; j <= ith; j++) {
//                assert i.getA() != chooser.get(j) && i.getB() != chooser.get(j);
//            }
//        }
        while (q.size() != 0) {
            boolean isRevised;
            solverSimpleVarPair i = q.poll();
            simpleVariable target = i.getA();
            simpleVariable compare = i.getB();
            try {
                isRevised = revise(target, compare);
            } catch (NoSolutionException e) {
                findWipeout = true;
                break;
            }
            if (isRevised) {
                target.getRefVar().constraints.stream()
                        .filter(j -> j.getArity() == 2)
                        .flatMap(j -> Arrays.stream(j.scope))
                        .filter(j -> !target.equals_VAR(j) && !compare.equals_VAR(j))
                        .map(j -> Solver.findSimpleVariable.get(j))
                        .filter(target_list::contains)
                        .distinct()
                        .forEach((j) -> {
                            solverSimpleVarPair x=new solverSimpleVarPair(j, target);
                            if(!q.contains(x)) q.offer(x);
                        });
            }
        }

        if (findWipeout) {
            undo_reductions_MAC();
            return true;
        }
        return false;
    }


    private Queue<solverSimpleVarPair> getInitQueue_queue() {
        Queue<solverSimpleVarPair> q = new ConcurrentLinkedQueue<>();
        Solver_MAC.getInitQueue_stream(target_list).forEach(q::offer);
        return q;
    }
}
