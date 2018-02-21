/*
 * CSCE421 2018Spr
 * Tao Yao
 * 09157432
 * Feb 9, 2018
 * */

package csp.tool;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import static java.lang.String.format;


public class solverReporter_ac {

    private String name;

    private String cc;

    private String cpu;

    private String fval;

    private String iSize;

    private String fSize;

    private String fEffect;

    private String file_name;

    public solverReporter_ac(String file_name, String name, int cc, long cpu, long fval, double iSize, double fSize, double fEffect) {
        this.file_name=file_name;
        this.name = name;
        this.cc = String.valueOf(cc);
        this.cpu = String.valueOf(cpu);
        this.fval = String.valueOf(fval);
        this.iSize = String.valueOf(iSize);
        this.fSize = String.valueOf(fSize);
        this.fEffect = String.valueOf(fEffect);
    }

    public void writeToFile(String addr){
        try {
            Writer writer = new FileWriter(addr,true);
            writer.write(name+","+cc+","+cpu+","+fval+","+iSize+","+fSize+","+fEffect+","+file_name+"\n");
            writer.close();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString(){
        String ret="";
        ret+=("cc: " + cc);
        ret+="\n";
        ret+=("cpu: " + cpu + "ms");
        ret+="\n";
        if(Double.valueOf(fSize)!=-1.0){
            ret+=("fval: " + fval);
            ret+="\n";
            ret+=format("iSize: %.5f",Double.valueOf(iSize));
            ret+="\n";
            ret+=format("fSize: %.5f",Double.valueOf(fSize));
            ret+="\n";
            ret+=format("fEffect: %.5f", Double.valueOf(fEffect));
        }
        else{
            ret+=("fval: " + fval+" (before discovering domain wipeout)");
            ret+="\n";
            ret+=format("iSize: %.5f",Double.valueOf(iSize));
            ret+="\n";
            ret+="fSize: false";
            ret+="\n";
            ret+="fEffect: false";
        }
        return ret;
    }
}
