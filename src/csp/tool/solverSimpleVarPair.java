package csp.tool;

import com.sun.org.apache.bcel.internal.classfile.Unknown;
import csp.data.simpleVariable;

public class solverSimpleVarPair{
    private simpleVariable a=null;
    private simpleVariable b=null;
    public solverSimpleVarPair(simpleVariable a, simpleVariable b){
        this.a=a;
        this.b=b;
    }

    @Override
    public boolean equals(Object x){
        if(x instanceof  solverSimpleVarPair){
            solverSimpleVarPair xx=(solverSimpleVarPair) x;
            if(xx.getA()==a&&xx.getB()==b){
                return true;
            }
        }
        return false;
    }

    public simpleVariable getA() {
        return a;
    }

    public simpleVariable getB() {
        return b;
    }

//    @Override
//    public int compareTo(Object o) {
//        solverSimpleVarPair t=(solverSimpleVarPair) o;
//        int ca=a.getName().compareTo(t.a.getName());
//        if(ca!=0){
//            return ca;
//        }
//        else{
//            return b.getName().compareTo(t.b.getName());
//        }
//    }

    @Override
    public int hashCode(){
        throw new UnknownError("Should not be reached");
//        return (int)(((long)a.hashCode()+b.hashCode())%10000000);
    }
}
