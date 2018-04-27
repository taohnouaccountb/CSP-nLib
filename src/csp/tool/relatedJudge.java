package csp.tool;

import com.sun.org.apache.xpath.internal.operations.Bool;
import csp.data.simpleVariable;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class relatedJudge {
    private Map<simpleVariable, Map<simpleVariable, Boolean> > relatedMark = null;
    public relatedJudge(){
        //TODO improve performance
    }

    public boolean isExist(simpleVariable a, simpleVariable b){
        return a.getRefVar().neighbors.contains(b.getRefVar());
    }
}
