package csp.tool.bt;

import csp.data.simpleVariable;
import csp.tool.relatedJudge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import csp.tool.bt.variableChooser.heuristicType;
import java.util.stream.Collectors;

public class staticVariableChooser {

    private List<simpleVariable> refOrg = null;
    public List<simpleVariable> li = null;
    private relatedJudge isRelated = null;
    private int[] degreeCounts;

    // Deprecated
    private List<simpleVariable> getDegreeOrder(List<simpleVariable> li) {
        degreeCounts = new int[li.size()];
        for (int i = 0; i < li.size(); i++) degreeCounts[i] = -1;
        List<simpleVariable> ret = new ArrayList<>();
        ArrayList<simpleVariable> l = new ArrayList<>();
        for (simpleVariable x : li) l.add(x);
        while (!l.isEmpty()) {
            int[] deg = new int[l.size()];
            for (int i = 0; i < l.size(); i++) {
                for (int j = 0; j < l.size(); j++) {
                    if (i != j) {
                        if (isRelated.isExist(l.get(i), l.get(j))) {
                            deg[i]++;
                        }
                    }
                }
            }
            int maxIndex = 0;
            for (int i = 0; i < deg.length; i++) {
                if (deg[i] == deg[maxIndex]) {
                    maxIndex = l.get(i).getName().compareTo(l.get(maxIndex).getName()) < 0 ? i : maxIndex;
                } else {
                    maxIndex = deg[i] > deg[maxIndex] ? i : maxIndex;
                }
            }
            degreeCounts[li.indexOf(l.get(maxIndex))] = deg[maxIndex];
            ret.add(l.get(maxIndex));
            l.remove(maxIndex);
        }
        return ret;
    }

    private List<simpleVariable> getDegreeOrder2(List<simpleVariable> li) {
        degreeCounts = new int[li.size()];
        List<simpleVariable> ret = new ArrayList<>();
        ArrayList<simpleVariable> l = new ArrayList<>();
        for (simpleVariable x : li) l.add(x);
        l.forEach(i -> i.deg = 0);
        for (simpleVariable i : li) {
            for (simpleVariable j : li) {
                if (i == j) continue;
                if (isRelated.isExist(i, j)) i.deg++;
            }
        }
        while (!l.isEmpty()) {
            simpleVariable maxRef = l.get(0);
            for (simpleVariable i : l) {
                if (i.deg==maxRef.deg) {
                    maxRef = i.getName().compareTo(maxRef.getName()) < 0 ? i : maxRef;
                } else {
                    maxRef = i.deg > maxRef.deg ? i : maxRef;
                }
            }
            degreeCounts[li.indexOf(maxRef)] = maxRef.deg;
            ret.add(maxRef);
            l.remove(maxRef);
            for(simpleVariable i:l) if(isRelated.isExist(maxRef, i)) i.deg--;
        }
        return ret;
    }

    private List<simpleVariable> genMWOrder(List<simpleVariable> li) {
        degreeCounts = new int[li.size()];
        List<simpleVariable> l = new ArrayList<>(li);
        List<simpleVariable> ret = new ArrayList<>();
        int k = 0;
        while (!l.isEmpty()) {
            l.forEach(i -> i.deg = 0);
            for (simpleVariable i : l) {
                for (simpleVariable j : l) {
                    if (i == j) continue;
                    if (isRelated.isExist(i, j)) i.deg++;
                }
            }
            int finalK = k;
            l.stream().filter(i -> i.deg <= finalK).forEach(i -> {
                ret.add(i);
                i.deg = finalK;
            });
            l.removeAll(ret);
            k++;
        }
        List<simpleVariable> rst = ret.stream().sorted((i, j) -> {
            int cmp = j.deg - i.deg;
            if (cmp == 0) return i.getName().compareTo(j.getName());
            return cmp;
        }).collect(Collectors.toList());

        return rst;
    }

    public staticVariableChooser(List<simpleVariable> orgLi, heuristicType mode, relatedJudge isRelated) {
        refOrg = orgLi;
        this.isRelated = isRelated;
        if (mode == heuristicType.LX) {
            this.li = orgLi.stream().sorted(Comparator.comparing(simpleVariable::getName))
                    .collect(Collectors.toList());
        } else if (mode == heuristicType.LD) {
            this.li = orgLi.stream().sorted((i, j) -> {
                int cmp = i.getCurrent_domain().size() - j.getCurrent_domain().size();
                if (cmp != 0) return cmp;
                else return i.getName().compareTo(j.getName());
            }).collect(Collectors.toList());
        } else if (mode == heuristicType.DEG) {
            this.li = getDegreeOrder2(orgLi);
        } else if (mode == heuristicType.DD) {
            getDegreeOrder2(orgLi);
//            for(int i=0;i<orgLi.size();i++) System.out.println(orgLi.get(i).getCurrent_domain().size()/(double)orgLi.get(i).deg);
            this.li = orgLi.stream().sorted((i, j) -> {
                double cmp = i.getCurrent_domain().size() / (double) i.deg - j.getCurrent_domain().size() / (double) j.deg;
                if (-0.000000001 < cmp && cmp < 0.000000001) return i.getName().compareTo(j.getName());
                else return cmp > 0 ? 1 : -1;
            }).collect(Collectors.toList());
//            this.li.forEach(System.out::println);
        } else if (mode == heuristicType.MWO) {
            this.li = genMWOrder(orgLi);
        } else {
            System.out.println("Unknown heuristic type");
            throw new java.lang.UnknownError();
        }
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
}
