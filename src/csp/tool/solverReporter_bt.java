package csp.tool;

import csp.data.simpleVariable;
import csp.tool.bt.staticVariableChooser;
import csp.tool.bt.variableChooser;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import static csp.MyParser.file_name;
import static java.lang.String.format;

public class solverReporter_bt {

    private List<String> column = new ArrayList<>();
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
    private String variable_order_heuristic;
    private variableChooser chooser;

    public solverReporter_bt(variableChooser chooser, String name, String BTtype, String variable_order_heuristic, String var_static_dynamic, String value_order_heuristic, String val_static_dynamic, int firstCc, int firstNv, int firstBt, long firstCpu, List<Integer> firstSolution, int cc, int nv, int bt, long cpu, int solutionNumbers) {
        if (variable_order_heuristic.equals("LX")) {
            variable_order_heuristic = "1 LX";
        } else if (variable_order_heuristic.equals("LD")) {
            variable_order_heuristic = "2 LD";
        } else if (variable_order_heuristic.equals("DEG")) {
            variable_order_heuristic = "3 DEG";
        } else if (variable_order_heuristic.equals("DD")) {
            variable_order_heuristic = "4 DD";
        } else if (variable_order_heuristic.equals("MWO")) {
            variable_order_heuristic = "5 MWO";
        } else if (variable_order_heuristic.equals("dLD")) {
            variable_order_heuristic = "6 dLD";
        } else if (variable_order_heuristic.equals("dDEG")) {
            variable_order_heuristic = "7 dDEG";
        } else if (variable_order_heuristic.equals("dDD")) {
            variable_order_heuristic = "8 dDD";
        }
        this.variable_order_heuristic = variable_order_heuristic;
        this.chooser = chooser;
        column.add(name);
        column.add(BTtype);
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

    public void writeToFile(String addr) {
        try {
            Writer writer = new FileWriter(addr, true);
            String line = file_name;
            for (int i = 0; i < column.size(); i++) {
                if (i == 9) continue;
                line += ", " + column.get(i);
            }
            line += "\n";
            writer.write(line);
            writer.close();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeToFileOrder(String addr) {
        try {
            Writer writer = new FileWriter(addr, true);
            String line = "";
            line += file_name + ", " + variable_order_heuristic;
            for (simpleVariable i : chooser.li) {
                line += ", " + i.getName();
            }
            writer.write(line + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        final String[] dict = {
                "Instance name: ",
                "Search: ",
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
        if (dict.length != column.size()) throw new java.lang.UnknownError("Wrong COLUMN data");

        String ret = "";
        for (int i = 0; i < dict.length; i++) {
            ret += dict[i] + column.get(i) + "\n";
        }

        return ret;
    }
}
