package csp.tool.bt;

import csp.data.simpleVariable;
import csp.tool.relatedJudge;

import java.util.*;
import csp.tool.bt.variableChooser.heuristicType;

public class dynamicVariableChooser {
    heuristicType mode;
    private List<simpleVariable> refOrg = null;
    public LinkedList<simpleVariable> li = null;
    private relatedJudge isRelated = null;
    private int[] degreeCounts;

    public dynamicVariableChooser(List<simpleVariable> orgLi, heuristicType mode, relatedJudge isRelated) {
        refOrg = orgLi;
        this.isRelated = isRelated;
        this.mode=mode;
        this.usedFlags= new HashMap<>();
        for(simpleVariable i:orgLi) this.usedFlags.replace(i,false);
        this.li=new LinkedList<>();
    }

    public simpleVariable get(int i) {
        return li.get(i);
    }

    public List<Integer> transSequence(int[] v) {
        List<Integer> ret = new ArrayList<>();
        for (simpleVariable i : refOrg) {
            ret.add(v[li.indexOf(i)]);
        }
        return ret;
    }

    public int getSize() {
        return li.size();
    }

    private Map<simpleVariable, Boolean> usedFlags;

    private boolean dynamicDomino(){
        return true;
    }

    private simpleVariable dynamicGetNext(heuristicType mode){
        // Apply domino effect
        if(dynamicDomino()){
            return li.getLast();
        }

        if (mode==heuristicType.dDD)
            return dDD();
        else if (mode==heuristicType.dLD)
            return dLD();
        else if (mode==heuristicType.dDEG)
            return dDEG();
        else
            throw new java.lang.UnknownError("UNKNOWN DYNAMIC HEURISTIC TYPE");
    }

    private List<simpleVariable> getUnusedVariables(){
        ArrayList<simpleVariable> pool=new ArrayList<>();
        for(simpleVariable i:refOrg){
            if(usedFlags.get(i)==null) throw new java.lang.UnknownError("UNKNOWN");
            if(usedFlags.get(i)) pool.add(i);
        }
        return pool;
    }
    private simpleVariable dDD(){
        return li.get(0);
    }
    private simpleVariable dLD(){
        List<simpleVariable> unused=getUnusedVariables();
        int least_index=0;
        for(int i=1;i<unused.size();i++){
            if(unused.get(i).getCurrent_domain().size()<unused.get(least_index).getCurrent_domain().size()){
                least_index=i;
            }
        }
        return unused.get(least_index);
    }
    private simpleVariable dDEG(){
        return li.get(0);
    }

    private int getPos(){
        return li.size()-1;
    }

    public simpleVariable next(){
        if(getPos()+1>refOrg.size()){
            throw new java.lang.UnknownError("POS EXCEED LIMIT");
        }
        simpleVariable nextVariable = dynamicGetNext(mode);
        li.add(nextVariable);
        usedFlags.replace(nextVariable,true);
        return nextVariable;
    }

    public void back(){
        usedFlags.replace(li.getLast(),false);
        li.removeLast();
    }
}
