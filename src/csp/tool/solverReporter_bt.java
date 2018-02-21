package csp.tool;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import static csp.MyParser.file_name;
import static java.lang.String.format;

public class solverReporter_bt {

    private List<String> column=new ArrayList<>();
//0    private String name;
//
//1    private String variable_order_heuristic;
//
//2    private String var_static_dynamic;
//
//3    private String value_order_heuristic;
//
//4   private String val_static_dynamic;
//
//5    private String firstCc;
//
//6    private String firstNv;
//
//7    private String firstBt;
//8    private String firstCpu;
//9    private String firstSolution;
//
//10    private String cc;
//11    private String nv;
//12    private String bt;
//13    private String cpu;
//14    private String solutionNumbers;
//
//15    private String file_name;

    public solverReporter_bt(String name, String variable_order_heuristic, String var_static_dynamic, String value_order_heuristic, String val_static_dynamic, int firstCc, int firstNv, int firstBt, long firstCpu, List<Integer> firstSolution, int cc, int nv, int bt, long cpu, int solutionNumbers) {
        column.add(name);
        column.add(variable_order_heuristic);
        column.add(var_static_dynamic);
        column.add(value_order_heuristic);
        column.add(val_static_dynamic);
        column.add(String.valueOf(firstCc));
        column.add(String.valueOf(firstNv));
        column.add(String.valueOf(firstBt));
        column.add(String.valueOf(firstCpu));
        column.add(String.valueOf(firstSolution));
        column.add(String.valueOf(cc));
        column.add(String.valueOf(nv));
        column.add(String.valueOf(bt));
        column.add(String.valueOf(cpu));
        column.add(String.valueOf(solutionNumbers));
    }

    public void writeToFile(String addr){
        try {
            Writer writer = new FileWriter(addr,true);
            String line="";
            for(String s:column) line+=s+",";
            line+=file_name+"\n";
            writer.write(line);
            writer.close();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString(){
        final String[] dict={
                "Instance name: ",
                "variable-order-heuristic: ",
                "var-static-dynamic: ",
                "value-ordering-heuristic: ",
                "val-static-dynamic: ",
                "cc: ",
                "nv: ",
                "bt: ",
                "cpu: ",
                "First solution: ",
                "all-sol cc: ",
                "all-sol nv: ",
                "all-sol bt: ",
                "all-sol cpu: ",
                "Number of solutions: "
        };
        if(dict.length!=column.size()) throw new java.lang.UnknownError("Wrong COLUMN data");

        String ret="";
        for(int i=0;i<dict.length;i++){
            ret+=dict[i]+column.get(i)+"\n";
        }

        return ret;
    }
}
