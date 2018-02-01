import abscon.instance.intension.EvaluationManager;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
        System.out.println(false||true);
       /* String[] expr={"X1","X0","ne"};
        int[] tuple={1,2};
        EvaluationManager evaluationManager = new EvaluationManager(expr);
        long result = evaluationManager.evaluate(tuple);
        System.out.println(result);


        int[] a={1,2,3};
        int[] b=a.clone();
        System.out.println("FUCK");
        LinkedList<Integer> list=new LinkedList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        Iterator<Integer> it=list.iterator();
        while(it.hasNext()){
            int x=it.next();
            if(x==2) it.remove();
            System.out.println(x);
        }

        it=list.iterator();
        while(it.hasNext()){
            int x=it.next();
            System.out.println(x);
        }
        List<Boolean> li=list.stream().map(i->i>=3).collect(Collectors.toList());
        for(Boolean i:li){
            System.out.println(i);
        }*/
    }

}
