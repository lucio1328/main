package util;

public class VerbAction {
    String methodeAction;
    String verb;
    
    public VerbAction(String methodeAction, String verb) {
        this.methodeAction = methodeAction;
        this.verb = verb;
    }
    public String getMethodeAction() {
        return methodeAction;
    }
    public void setMethodeAction(String methodeAction) {
        this.methodeAction = methodeAction;
    }
    public String getVerb() {
        return verb;
    }
    public void setVerb(String verb) {
        this.verb = verb;
    }

    /* Tester verb */
    public static int testVerbAction(String methodAction, String verb) {
        if(methodAction.equalsIgnoreCase(methodAction) && verb.equalsIgnoreCase(verb)){
            return 0;
        }
        return 1;
    }
}
