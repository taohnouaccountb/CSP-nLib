package csp.tool.bt;

import csp.data.simpleVariable;
import csp.tool.relatedJudge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class variableChooser {
    public enum heuristicType {LX, LD, DEG, UU, DD}

    private List<simpleVariable> refOrg =null;
    private List<simpleVariable> li =null;
    private relatedJudge isRelated = null;

    private List<simpleVariable> getDegreeOrder(List<simpleVariable> li){
        List<simpleVariable> ret=new ArrayList<>();
        ArrayList<simpleVariable> l=new ArrayList<>();
        for(simpleVariable x:li) l.add(x);
        while(!l.isEmpty()){
            int[] deg=new int[l.size()];
            for(int i=0;i<l.size();i++){
                for(int j=0;j<l.size();j++){
                    if(i!=j){
                        if(isRelated.isExist(l.get(i),l.get(j))){
                            deg[i]++;
                        }
                    }
                }
            }
            int max=0;
            for(int i=0;i<deg.length;i++){
                if(deg[i]==deg[max]){
                    max=l.get(i).getName().compareTo(l.get(max).getName())<0?i:max;
                }
                else{
                    max=deg[i]>deg[max]?i:max;
                }
            }
            ret.add(l.get(max));
            l.remove(max);
        }
        return ret;
    }

    public variableChooser(List<simpleVariable> orgLi, heuristicType mode, relatedJudge isRelated){
        refOrg = orgLi;
        this.isRelated = isRelated;
        if(mode==heuristicType.LX){
            this.li =orgLi.parallelStream().sorted(Comparator.comparing(simpleVariable::getName))
                    .collect(Collectors.toList());
        }
        else if(mode==heuristicType.LD){
            this.li =orgLi.parallelStream().sorted(Comparator.comparingInt(i -> i.getCurrent_domain().size()))
                    .collect(Collectors.toList());
        }
        else if(mode==heuristicType.DEG){
            this.li=getDegreeOrder(orgLi);
        }
        else if(mode==heuristicType.DD){
            int[] deg=new int[orgLi.size()];
            for(int i=0;i<orgLi.size();i++){
                for(int j=0;j<orgLi.size();j++){
                    if(i!=j){
                        if(isRelated.isExist(orgLi.get(i),orgLi.get(j))){
                            deg[i]++;
                        }
                    }
                }
            }
            for(int i=0;i<orgLi.size();i++) orgLi.get(i).deg=deg[i];
            this.li =orgLi.parallelStream().sorted(Comparator.comparingDouble(i -> i.getCurrent_domain().size()*1.0/i.deg))
                    .collect(Collectors.toList());
        }
        else{
            System.out.println("Unknown heuristic type");
            throw new java.lang.UnknownError();
        }
    }

    public simpleVariable get(int i){
        return li.get(i);
    }

    public List<Integer> transSequence(int[] v){
        List<Integer> ret=new ArrayList<>();
        for(simpleVariable i:refOrg){
            ret.add(v[li.indexOf(i)]);
        }
        return ret;
    }

    public int getSize(){
        return li.size();
    }
}
