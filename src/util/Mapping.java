package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Mapping {
    String nameClass;
    String nameMethod; 
    List<VerbAction> verbActions = new ArrayList<>();
    
    public Mapping(String nameClass, String nameMethod, VerbAction verbAction) {
        this.nameClass = nameClass;
        this.nameMethod = nameMethod;
        this.verbActions.add(verbAction);
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

    public List<VerbAction> getVerbActions() {
        return verbActions;
    }
    public void setVerbActions(List<VerbAction> verbActions) {
        this.verbActions = verbActions;
    }
}
