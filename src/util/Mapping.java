package util;

import java.util.HashMap;
import java.util.Map;

public class Mapping {
    String nameClass;
    String nameMethod; 
    String verb;
    
    public Mapping(String controlleur, String methode, String verb) {
        this.nameClass = controlleur;
        this.nameMethod = methode;
        this.verb = verb;
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

    public String getVerb() {
        return verb;
    }
    public void setVerb(String verb) {
        this.verb = verb;
    }
}
