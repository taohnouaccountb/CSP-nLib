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

import java.util.ArrayList;
import java.util.Iterator;
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
                if(args[i+1].equals("ac1")){
                    Solver solve=new Solver();
                    solve.init(problem);
                    solve.NC();
                    solve.AC1();
                }
            }
        }
        if(!validArg){
            parser = new MyParser("./data.xml");
            file_name="data";
            Solver solver=new Solver();
            solver.init(problem);
            solver.NC();
            solver.AC1();
        }

    }
}
