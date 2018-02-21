package csp.tool;

import csp.data.simpleVariable;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class relatedJudge {
    private Set<solverSimpleVarPair> relatedMark = null;
    public relatedJudge(Stream<solverSimpleVarPair> ss){
        relatedMark = ss.collect(Collectors.toSet());
    }
    public boolean isExist(simpleVariable a, simpleVariable b){
        return relatedMark.contains(new solverSimpleVarPair(a,b));
    }
}
