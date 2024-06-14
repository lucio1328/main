package util;

import java.util.HashMap;
import java.util.Map;

public class Mapping {
    String nameClass;
    String nameMethod; 
    public Mapping(String controlleur, String methode) {
        this.nameClass = controlleur;
        this.nameMethod = methode;
    }
    public Mapping(){

    }
    public void add(String n1, String n2) {
        this.nameClass = n1;
        this.nameMethod = n2;
    }

    public String getValue() {
        return nameMethod;
    }

    public String getKey(){
        return nameClass;
    }
}
