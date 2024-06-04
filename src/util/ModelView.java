package util;

import java.util.HashMap;

public class ModelView {
    /* Attributs */
    String url;
    HashMap<String, Object> data;

    /* COnstructors */
    public ModelView(String url) {
        setUrl(url);
    }
    public ModelView() {
        data = new HashMap<>();
    }
    /* Getters et Setters */
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public HashMap<String, Object> getData() {
        return data;
    }
    public void setData(HashMap<String, Object> data) {
        this.data = data;
    }

    /* Add object */
    public void addObject(String nomVar, Object valeur) {
        data.put(nomVar, valeur);
    }

    /* get object */
    public Object getObject(){
        Object o = (Object) data.values();
        return o;
    }
}
