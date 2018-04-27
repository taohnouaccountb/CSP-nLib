package csp.io;
import java.util.Random;


public class random {
    private static Random p=null;
    public static int randint(int size){
        if(p==null) p = new Random();
        return p.nextInt(size);
    }
}
