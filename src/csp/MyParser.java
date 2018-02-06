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
import csp.tool.solverReporter;

import java.util.ArrayList;
import java.util.List;


public class MyParser {
    static private List<Variable> variables;
    static public Constraint constraints;
    static public Problem problem;
    static public String file_name;

    public MyParser(String filename) {
        InstanceParser parser = new InstanceParser();
        parser.loadInstance(filename);
        parser.parse(false);
        variables = new ArrayList<Variable>();

        //Variables

        for (int i = 0; i < parser.getVariables().length; i++) {
            Variable newVar = new Variable(parser.getVariables()[i]);
            variables.add(newVar);
        }

        //Constraints

        for (String key : parser.getMapOfConstraints().keySet()) {
            Constraint con = null;
            try {
                con = Constraint.genConstraint(parser.getMapOfConstraints().get(key));
                if(constraints==null){
                    constraints=con;
                }
                else{
                    constraints.add(con);
                    constraints=constraints.getNext();
                }
            } catch (DatatypeMisMachthing e) {
                e.printStackTrace();
            } catch (DataAccessError e){
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
                S.solve(Solver.SOLUTIONS.NC);

                //Solve
                solverReporter result=null;
                if(args[i+1].equals("ac1")){
                    result=S.solve(Solver.SOLUTIONS.AC1);
                }
                else if(args[i+1].equals("ac3")){
                    result=S.solve(Solver.SOLUTIONS.AC3);
                }
                else if(args[i+1].equals("ac2001")){
                    result=S.solve(Solver.SOLUTIONS.AC2001);
                }

                //Output
                System.out.println(result);
                if(needWrite) result.writeToFile("solver_output.csv");
            }
            else if(args[i].equals("-r")){
                needWrite=true;
            }
        }
        if(!validArg){
            //Default
            parser = new MyParser("./data.xml");
            file_name="data";
            Solver S=new Solver();
            S.init(problem);
            S.solve(Solver.SOLUTIONS.NC);
            solverReporter result=S.solve(Solver.SOLUTIONS.AC2001);
            result.writeToFile("solver_output.csv");
            System.out.println(result);
        }

    }
}
