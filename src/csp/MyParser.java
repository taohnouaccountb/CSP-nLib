/*
* CSCE421 2018Spr
* Tao Yao
* 09157432
* Jan. 21, 2018
* */

package csp;

import abscon.instance.tools.InstanceParser;
import csp.data.*;
import csp.tool.sortPrint;

import java.util.ArrayList;
import java.util.List;


public class MyParser {
    static private List<Variable> variables;
    static public Constraint constraints;
    static public Problem problem;

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
        MyParser parser;
        if (args.length == 2 && args[0].equals("-f")) {
            parser = new MyParser(args[1]);
        } else {
            parser = new MyParser("./data.xml");
        }
    }
}
