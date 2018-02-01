package csp.io;

import java.util.ArrayList;
import java.util.List;

public class sortPrint {
    static private int compareNameString(String a,String b){
        int aa=Integer.valueOf(a.substring(7,a.lastIndexOf(", variables")));
        int bb=Integer.valueOf(b.substring(7,b.lastIndexOf(", variables")));
        if(aa>bb) return 1;
        else return -1;
    }
    private List<String> arr = null;
    private List<Integer> arrInt = null;
    private String head;

    private enum SPE {STRING, INT,NAME}

    SPE type = SPE.STRING;

    public void addString(String s) {
        if (s.charAt(0) == 'C'||s.charAt(0) == 'V') {
            type = SPE.INT;
            head=String.valueOf(s.charAt(0));
        }
        if (s.startsWith("Name: C")){
            type = SPE.NAME;
        }

        if (arr == null) {
            arr = new ArrayList<String>();
            arrInt = new ArrayList<Integer>();
        }
        if (type == SPE.INT) {
            arrInt.add(Integer.valueOf(s.substring(1, s.length())));
        } else {
            arr.add(s);
        }
    }


    public void addString(Iterable arr) {
        for (Object s : arr) {
            addString(s.toString());
        }
    }

    public String toString(int signal) {
        String ret = "";
        if(type==SPE.NAME)
            arr.sort(sortPrint::compareNameString);
        else arr.sort(String::compareTo);
        arrInt.sort(Integer::compareTo);
        boolean flag = true;
        if(type==SPE.INT){
            for (Integer s : arrInt) {
                if (flag) {
                    flag = false;
                } else {
                    if (signal == 0) ret += ",";
                    else ret += "\n";
                }
                ret += head;
                ret+=s;
            }
        }
        else{
            for (String s : arr) {
                if (flag) {
                    flag = false;
                } else {
                    if (signal == 0) ret += ",";
                    else ret += "\n";
                }
                ret += s;
            }

        }
        return ret;
    }

    public void clear() {
        arr = null;
        type = SPE.STRING;
    }

}
