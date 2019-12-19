package org.madgik.io.modality;

public abstract class Modality {
    public enum type{
        text("text", Text.class), keywords("keywords", Keywords.class), mesh("mesh", Mesh.class), dbpedia("dbpedia", DBPedia.class);
        String name;
        Class cls;
        type(String name, Class cls){
            this.name = name;
            this.cls = cls;
        }
        public static Class modalityClassName(String name){
            for (type t: type.values()){
               if (t.name.equals(name))  return t.cls;
            }
            return null;
        }
    }
    public static String text(){    return type.text.name();}
    public static String keywords(){return type.keywords.name();}
    public static String mesh(){    return type.mesh.name();}
    public static String dbpedia(){ return type.dbpedia.name();}

    String id;

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Modality(String id, String content) {
        this.id = id;
        this.content = content;
    }

    String content;
}
