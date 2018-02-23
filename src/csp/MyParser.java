/*
* CSCE421 2018Spr
* Tao Yao
* 09157432
* Jan. 21, 2018
* */

package csp;

import abscon.instance.tools.InstanceParser;
import csp.data.*;
import csp.io.sortPrint;
import csp.tool.Solver;
import csp.tool.bt.variableChooser;
import csp.tool.solverReporter_ac;
import csp.tool.solverReporter_bt;

import java.util.ArrayList;
import java.util.List;

import static csp.tool.Solver.SOLUTIONS_bt.BT;


public class MyParser {
    static private List<Variable> variables;
    static public List<Constraint> constraints;
    static public Problem problem;
    static public String file_name;

    public MyParser(String filename) {
        InstanceParser parser = new InstanceParser();
        parser.loadInstance(filename);
        parser.parse(false);
        variables = new ArrayList<>();
        constraints = new ArrayList<>();

        //Variables

        for (int i = 0; i < parser.getVariables().length; i++) {
            Variable newVar = new Variable(parser.getVariables()[i]);
            variables.add(newVar);
        }

        //Constraints
        for (String key : parser.getMapOfConstraints().keySet()) {
            try {
                Constraint con = Constraint.genConstraint(parser.getMapOfConstraints().get(key));
                constraints.add(con);
            } catch (DatatypeMisMachthing e) {
                e.printStackTrace();
            }
        }
//        printParser(parser);
        problem=new Problem(parser.getName(),variables,constraints);
    }

    private void printParser(InstanceParser parser){
        sortPrint sp=new sortPrint();
        System.out.println("Instance name: "+parser.getName());

        System.out.println("Variables:");
        sp.clear();
        sp.addString(variables);
        System.out.println(sp.toString(1));

        System.out.println("Constraints:");
        sp.clear();
        sp.addString(constraints.toString());
        sp.addString(constraints);
        System.out.println(sp.toString(1));
    }


    public static void main(String[] args) {
// Hardcoded now... but should read in the file through the arguments, -f <XML-NAME>
        MyParser parser = null;

        boolean validArg=false;
        boolean needWrite=false;
        variableChooser.heuristicType heuristicType=null;

        for(String s:args) if(s.equals("-r")) needWrite=true;
        for(int i=0;i<args.length;i++){
            if(args[i].equals("-f")){
                validArg=true;
                if(parser!=null){
                    System.out.println("ERROR: Multiple '-f' detected.");
                    break;
                }
                file_name=args[i+1].substring(args[i+1].lastIndexOf("\\")+1,args[i+1].length()-5);
                parser = new MyParser(args[i+1]);
            }
            else if(args[i].equals("-a")){
                validArg=true;
                if(parser==null){
                    System.out.println("ERROR: '-f' should be the first argument.");
                    break;
                }

                //Preprocess
                Solver S=new Solver();
                S.init(problem);
                S.solve_ac(Solver.SOLUTIONS_ac.NC);

                //Solve
                solverReporter_ac result=null;
                if(args[i+1].equals("ac1")){
                    result=S.solve_ac(Solver.SOLUTIONS_ac.AC1);
                }
                else if(args[i+1].equals("ac3")){
                    result=S.solve_ac(Solver.SOLUTIONS_ac.AC3);
                }
                else if(args[i+1].equals("ac2001")){
                    result=S.solve_ac(Solver.SOLUTIONS_ac.AC2001);
                }

                //Output
                System.out.println(result);
                if(needWrite) result.writeToFile("solver_output.csv");
            }
            else if(args[i].equals("-s")){
                for(int k=0;k<args.length;k++){
                    if(args[k].equals("-u")){
                        if(args[k+1].equals("LX")){
                            heuristicType= variableChooser.heuristicType.LX;
                        }
                        else if(args[k+1].equals("LD")){
                            heuristicType= variableChooser.heuristicType.LD;
                        }
                        else if(args[k+1].equals("DEG")){
                            heuristicType= variableChooser.heuristicType.DEG;
                        }
                        else if(args[k+1].equals("DD")){
                            heuristicType= variableChooser.heuristicType.DD;
                        }
                        else if(args[k+1].equals("WMO")){
                            heuristicType= variableChooser.heuristicType.WMO;
                        }
                        break;
                    }
                }
                if(heuristicType==null) heuristicType= variableChooser.heuristicType.LX;
                Solver S=new Solver();
                S.init(problem);
                S.solve_ac(Solver.SOLUTIONS_ac.NC);
                S.AC_trim();

                //solve
                solverReporter_bt result=null;
                if(args[i+1].equals("BT")){
                    result=S.solve_bt(Solver.SOLUTIONS_bt.BT,heuristicType);
                    System.out.print(result);
                    if(needWrite){
                        result.writeToFile("solver_output.csv");
                        result.writeToFileOrder("solver_output_ord.csv");
                    }

                }
                else{
                    System.out.println("Wrong Parameter of '-s'");
                }
            }
        }
        if(!validArg){
            //Default
            parser = new MyParser("./data-zebra.xml");
            file_name="data";
            Solver S=new Solver();
            S.init(problem);
            S.solve_ac(Solver.SOLUTIONS_ac.NC);
            S.AC_trim();
            solverReporter_bt result=S.solve_bt(Solver.SOLUTIONS_bt.BT,variableChooser.heuristicType.DD);
            result.writeToFile("solver_output.csv");
            result.writeToFileOrder("solver_output_ord.csv");
            System.out.println(result);
        }

    }
}
