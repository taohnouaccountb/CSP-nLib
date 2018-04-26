package csp.tool.bt;

import csp.data.Constraint;
import csp.data.Variable;
import csp.data.simpleVariable;
import csp.tool.relatedJudge;

import java.util.*;
import java.util.stream.Collectors;

public class dynamicVariableChooser extends variableChooser{
    private List<simpleVariable> unusedMemory = null;

    public dynamicVariableChooser(List<simpleVariable> orgLi, heuristicType mode, relatedJudge isRelated) {
        super(orgLi,mode,isRelated);
        refOrg = orgLi;
        this.isRelated = isRelated;
        this.mode=mode;
        this.usedFlags= new HashMap<>();
        for(simpleVariable i:orgLi) this.usedFlags.put(i,false);
        this.li=new LinkedList<>();
    }

    private Map<simpleVariable, Boolean> usedFlags;

    private simpleVariable dynamicDomino(List<simpleVariable> unused){
        for(simpleVariable i:unused){
            if(i.getCurrent_domain().size()==1){
//                System.out.println("DOMINO ACTIVATED!");
                return i;
            }
        }
        return null;
    }

    private simpleVariable dynamicGetNext(heuristicType mode){
        // Apply domino effect
        List<simpleVariable> unused=getUnusedVariables(-233);
        simpleVariable fastChoose=dynamicDomino(unused);
        if(fastChoose!=null){
            return fastChoose;
        }
        if (mode==heuristicType.dLD){
            return dLD();
        }
        else if (mode==heuristicType.dDD){
            return getDegreeOrder2plus(unused);
        }
        else if (mode==heuristicType.dDEG){
            return getDegreeOrder2s(unused);
        }
        else if (mode==heuristicType.dDwD){
            return getDegreeOrderDwD(unused);
        }
        else{
            throw new java.lang.UnknownError("UNKNOWN DYNAMIC HEURISTIC TYPE");
        }
    }

    @Override
    public List<simpleVariable> getUnusedVariables(int pos){
        if(pos!=count&&pos!=-233) throw new java.lang.UnknownError("WRONG INDEX");

        if (unusedMemory!=null) return unusedMemory;
        ArrayList<simpleVariable> pool=new ArrayList<>();
        for(simpleVariable i:refOrg){
            if(usedFlags.get(i)==null) throw new java.lang.UnknownError("UNKNOWN");
            if(usedFlags.get(i)==false) pool.add(i);
        }
        unusedMemory = pool;
        // the returned value should not be changed
        return pool;
    }

    //Get the unused variable that has the largest degree
    private simpleVariable getDegreeOrder2s(List<simpleVariable> li) {
        // Initialize
        ArrayList<simpleVariable> l = new ArrayList<>();
        for (simpleVariable x : li) l.add(x);
        l.forEach(i -> i.deg = 0);

        // Count initial degrees
        for (simpleVariable i : li) {
            for (simpleVariable j : li) {
                if (i == j) continue;
                if (isRelated.isExist(i, j)) i.deg++;
            }
        }

        // Select the variable
        simpleVariable maxRef = l.get(0);
        for (simpleVariable i : l) {
            if (i.deg==maxRef.deg) {
                maxRef = i.getName().compareTo(maxRef.getName()) < 0 ? i : maxRef;
            } else {
                maxRef = i.deg > maxRef.deg ? i : maxRef;
            }
        }
        return maxRef;
    }

    // Get the degrees of all unused variables
    private simpleVariable getDegreeOrder2plus(List<simpleVariable> list) {
        // Initialize
        int[] degreeCounts = new int[list.size()];
        ArrayList<simpleVariable> l = new ArrayList<>();
        for (simpleVariable x : list) l.add(x);
        l.forEach(i -> i.deg = 0);

        // Count initial degrees
        for (simpleVariable i : list) {
            for (simpleVariable j : list) {
                if (i == j) continue;
                if (isRelated.isExist(i, j)) i.deg++;
            }
        }

        // Count future-degrees
        while (!l.isEmpty()) {
            simpleVariable maxRef = l.get(0);
            for (simpleVariable i : l) {
                if (i.deg==maxRef.deg) {
                    maxRef = i.getName().compareTo(maxRef.getName()) < 0 ? i : maxRef;
                } else {
                    maxRef = i.deg > maxRef.deg ? i : maxRef;
                }
            }
            degreeCounts[list.indexOf(maxRef)] = maxRef.deg;
            l.remove(maxRef);
            for(simpleVariable i:l) if(isRelated.isExist(maxRef, i)) i.deg--;
        }

        // Select the variable has smallest 'dom/deg'
        int least_index=0;
        for(int i=1;i<list.size();i++){
            double len_i = list.get(i).getCurrent_domain().size()*1.0/degreeCounts[i];
            double len_least = list.get(least_index).getCurrent_domain().size()*1.0/degreeCounts[least_index];
            if(len_i==len_least&&list.get(i).getName().compareTo(list.get(least_index).getName())<0){
                least_index=i;
            }
            else if(len_i<len_least){
                least_index=i;
            }
        }
        return list.get(least_index);
    }

    static private simpleVariable getDegreeOrderDwD(List<simpleVariable> unused) {
        // Initialize
        ArrayList<simpleVariable> l = new ArrayList<>();
        for (simpleVariable x : unused) l.add(x);

        // Count weight-degrees
        List<Variable> unusedVariableList = unused.stream().map(simpleVariable::getRefVar).distinct().collect(Collectors.toList());
        for(simpleVariable i: unused){
            int sum=0;
            for(Constraint con: i.getRefVar().constraints){
                if(con.getArity()!=2) continue;
                boolean inRange= unusedVariableList.contains(con.scope[0]) && unusedVariableList.contains(con.scope[1]);
                if(!inRange) {
                    continue;
                }
                sum+=con.wdeg;
            }
            i.deg=sum;
        }

        // Select the variable has smallest 'dom/wdeg'
        simpleVariable least_variable=unused.get(0);

        for(simpleVariable i:unused){
            double len_i = i.getCurrent_domain().size()*1.0/i.deg;
            double len_least = least_variable.getCurrent_domain().size()*1.0/least_variable.deg;
            if(len_i==len_least&&i.getName().compareTo(least_variable.getName())<0){
                least_variable=i;
            }
            else if(len_i<len_least){
                least_variable=i;
            }
        }
        return least_variable;
    }

    private simpleVariable dLD(){
        List<simpleVariable> unused=getUnusedVariables(-233);
        // Choose the variable has least domain size
        int least_index=0;
        for(int i=1;i<unused.size();i++){
            int li = unused.get(i).getCurrent_domain().size();
            int ll = unused.get(least_index).getCurrent_domain().size();
            if(li==ll&&unused.get(i).getName().compareTo(unused.get(least_index).getName())<0){
                least_index=i;
            }
            else if(li<ll){
                least_index=i;
            }
        }
        return unused.get(least_index);
    }

    // Get current position of dynamic choosing
    private int getPos(){
        return li.size()-1;
    }

    // Choose next variable and add it to the list
    @Override
    public void next(){
        unusedMemory = null;
        count++;
        if(getPos()+1>refOrg.size()){
            throw new java.lang.UnknownError("POS EXCEED LIMIT");
        }
        simpleVariable nextVariable = dynamicGetNext(mode);
        li.add(nextVariable);
        usedFlags.replace(nextVariable,true);
    }

    // Discard the last variable
    @Override
    public void back(){
        unusedMemory = null;
        count--;
        usedFlags.replace(((LinkedList<simpleVariable>)li).getLast(),false);
        ((LinkedList<simpleVariable>)li).removeLast();
    }
}
