package org.madgik.dbpediaspotlightclient;
import org.madgik.io.modality.Text;

/**
 * Poison indicating thread should die.
 * @author mhorst
 *
 */
public class PubTextPoison extends Text {

    public PubTextPoison() {
        super(null, null);
    }

}
