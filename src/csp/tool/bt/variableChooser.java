package csp.tool.bt;

import csp.data.simpleVariable;
import csp.tool.relatedJudge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class variableChooser {
    public enum heuristicType {LX, LD, DEG, UU, DD, WMO}

    private List<simpleVariable> refOrg =null;
    public List<simpleVariable> li =null;
    private relatedJudge isRelated = null;
    private int[] degreeCounts;
    private List<simpleVariable> getDegreeOrder(List<simpleVariable> li){
        degreeCounts=new int[li.size()];
        for(int i=0;i<li.size();i++) degreeCounts[i]=-1;
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
            int maxIndex=0;
            for(int i=0;i<deg.length;i++){
                if(deg[i]==deg[maxIndex]){
                    maxIndex=l.get(i).getName().compareTo(l.get(maxIndex).getName())<0?i:maxIndex;
                }
                else{
                    maxIndex=deg[i]>deg[maxIndex]?i:maxIndex;
                }
            }
            degreeCounts[li.indexOf(l.get(maxIndex))]=deg[maxIndex];
            ret.add(l.get(maxIndex));
            l.remove(maxIndex);
        }
        return ret;
    }

    private List<simpleVariable> genMWOrder(List<simpleVariable> li){
        degreeCounts = new int[li.size()];
        List<simpleVariable> l = new ArrayList<>(li);
        List<simpleVariable> ret = new ArrayList<>();
        int k=0;
        while(!l.isEmpty()){
            l.forEach(i->i.deg=0);
            for(simpleVariable i:l){
                for(simpleVariable j:l){
                    if(i==j) continue;
                    if(isRelated.isExist(i,j)) i.deg++;
                }
            }
            int finalK = k;
            l.stream().filter(i->i.deg<= finalK).forEach(i->{
                ret.add(i);
                i.deg=finalK;
            });
            l.removeAll(ret);
            k++;
        }
        List<simpleVariable> rst=ret.stream().sorted((i,j)->{
           int cmp=i.deg-j.deg;
           if(cmp==0) return i.getName().compareTo(j.getName());
           return -cmp;
        }).collect(Collectors.toList());

        return rst;
    }

    public variableChooser(List<simpleVariable> orgLi, heuristicType mode, relatedJudge isRelated){
        refOrg = orgLi;
        this.isRelated = isRelated;
        if(mode==heuristicType.LX){
            this.li =orgLi.stream().sorted(Comparator.comparing(simpleVariable::getName))
                    .collect(Collectors.toList());
        }
        else if(mode==heuristicType.LD){
            this.li =orgLi.stream().sorted((i,j) ->{
                int cmp=i.getCurrent_domain().size()-j.getCurrent_domain().size();
                if(cmp!=0) return cmp;
                else return i.getName().compareTo(j.getName());
            }).collect(Collectors.toList());
        }
        else if(mode==heuristicType.DEG){
            this.li=getDegreeOrder(orgLi);
        }
        else if(mode==heuristicType.DD){
            getDegreeOrder(orgLi);
            for(int i=0;i<orgLi.size();i++) orgLi.get(i).deg=degreeCounts[i];
//            for(int i=0;i<orgLi.size();i++) System.out.println(orgLi.get(i).getCurrent_domain().size()/(double)orgLi.get(i).deg);
            /*degreeCounts=new int[orgLi.size()];
            for(int i=0;i<orgLi.size();i++){
                for(int j=0;j<orgLi.size();j++){
                    if(i==j) continue;
                    if(isRelated.isExist(orgLi.get(i),orgLi.get(j))){
                        orgLi.get(i).deg++;
                    }
                }
            }*/
            this.li =orgLi.stream().sorted((i,j) ->{
                double cmp=i.getCurrent_domain().size()/(double)i.deg-j.getCurrent_domain().size()/(double)j.deg;
                if(-0.000000001<cmp && cmp<0.000000001) return i.getName().compareTo(j.getName());
                else return cmp>0?1:-1;
            })
            .collect(Collectors.toList());
//            this.li.forEach(System.out::println);
        }
        else if(mode==heuristicType.WMO){
            this.li = genMWOrder(orgLi);
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
