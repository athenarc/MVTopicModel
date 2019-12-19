package org.madgik.dbpediaspotlightclient;

public class DBpediaLink {
    public String label;
    public String uri;

    public DBpediaLink(String uri, String label) {
        this.label = label;
        this.uri = uri;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        DBpediaLink guest = (DBpediaLink) obj;
        return uri.equals(guest.uri);
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

}
