package csp.tool;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;

public class solverReporter {
    @CsvBindByPosition(position = 0)
    public String name;

    @CsvBindByPosition(position = 1)
    public String cc;

    @CsvBindByPosition(position = 2)
    public String cpu;

    @CsvBindByPosition(position = 3)
    public String fval;

    @CsvBindByPosition(position = 4)
    public String iSize;

    @CsvBindByPosition(position = 5)
    public String fSize;

    @CsvBindByPosition(position = 6)
    public String fEffect;

    @CsvBindByPosition(position = 7)
    public String file_name;

    public String getFile_name() {
        return file_name;
    }

    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCc() {
        return cc;
    }

    public void setCc(String cc) {
        this.cc = cc;
    }

    public String getCpu() {
        return cpu;
    }

    public void setCpu(String cpu) {
        this.cpu = cpu;
    }

    public String getFval() {
        return fval;
    }

    public void setFval(String fval) {
        this.fval = fval;
    }

    public String getiSize() {
        return iSize;
    }

    public void setiSize(String iSize) {
        this.iSize = iSize;
    }

    public String getfSize() {
        return fSize;
    }

    public void setfSize(String fSize) {
        this.fSize = fSize;
    }

    public String getfEffect() {
        return fEffect;
    }

    public void setfEffect(String fEffect) {
        this.fEffect = fEffect;
    }

    public solverReporter(String file_name,String name, int cc, long cpu, long fval, double iSize, double fSize, double fEffect) {
        this.file_name=file_name;
        this.name = name;
        this.cc = String.valueOf(cc);
        this.cpu = String.valueOf(cpu);
        this.fval = String.valueOf(fval);
        this.iSize = String.valueOf(iSize);
        this.fSize = String.valueOf(fSize);
        this.fEffect = String.valueOf(fEffect);
    }
}
