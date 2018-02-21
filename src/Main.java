/*
 * CSCE421 2018Spr
 * Tao Yao
 * 09157432
 * Feb 9, 2018
 * */
import java.util.*;

public class Main {

    public static void main(String[] args) {
        System.out.println("a".compareTo("b"));
//        String[] expr={"X1","X0","ne"};
        int[] tuple={1,2};
        int[][] tuples={{1,2},{3,4}};
        List<Integer> list=new ArrayList<Integer>();
        list.add(1);
        list.add(2);
        list.remove(Integer.valueOf(1));
        System.out.println(list);

//        Arrays.stream(tuples).flatMapToInt(i->Arrays.stream(i)).forEach(System.out::println);
//
//        String[][] data = new String[][]{{"a", "b"}, {"c", "d"}, {"e", "f"}};
//
//        //Stream<String[]>
//        Stream<String[]> temp = Arrays.stream(data);
//
//        //Stream<String>, GOOD!
//        Stream<String> stringStream = temp.flatMap(x -> Arrays.stream(x));
//
//        stringStream.forEach(System.out::println);
//        EvaluationManager evaluationManager = new EvaluationManager(expr);
//        long result = evaluationManager.evaluate(tuple);
//        System.out.println(result);
//
//
//        int[] a={1,2,3};
//        int[] b=a.clone();
//        System.out.println("FUCK");
//        LinkedList<Integer> list=new LinkedList<>();
//        list.add(1);
//        list.add(2);
//        list.add(3);
//        list.add(4);
//        Iterator<Integer> it=list.iterator();
//        while(it.hasNext()){
//            int x=it.next();
//            if(x==2) it.remove();
//            System.out.println(x);
//        }
//
//        it=list.iterator();
//        while(it.hasNext()){
//            int x=it.next();
//            System.out.println(x);
//        }
//        List<Boolean> li=list.stream().map(i->i>=3).collect(Collectors.toList());
//        for(Boolean i:li){
//            System.out.println(i);
//        }
    }

}
