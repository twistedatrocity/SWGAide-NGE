package swg.model;

import java.io.File;
import java.io.Serializable;

import swg.gui.common.SWGGui;
import swg.tools.ZReader;

/**
 * This is a wrapper type for a station's "macros.txt" file. The macros file is
 * updated by the in-game Macro editor.
 * <p>
 * This type was reworked in November 2010 to make it host independent. Because
 * it does not contain anything that must be saved in SWGAide's DAT file it is
 * now prepared for being transient. A station no longer stores this type during
 * serialization, but this type will supports serialization for yet some time so
 * users with old DAT files can load these without troubles. XXX: remove
 * serialization (this is added for 0.9.0), I decided to not add a readResolve
 * method because deserialized instances are garbage collected and stations
 * create new instances on demand.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public class SWGMacros implements Serializable, SWGGui {

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = -8917519242489926174L;

    /**
     * The absolute file path for this instance.
     */
    private final File file;

    /**
     * Constructs a wrapper object around the "macros.txt" file. Notice that
     * this is a session scope instance, it is no longer saved with a station.
     * 
     * @param station the station for this instance
     */
    SWGMacros(SWGStation station) {
        file = new File(station.swgPath(), "macros.txt");
    }

    /**
     * Returns the macros text as a string. This method returns "Missing file on
     * computer" if that is appropriate. If the file is empty the empty string
     * is returned. If there is an error it an error message is returned.
     * 
     * @return the macros text, or an appropriate string
     */
    public String content() {
            String s = ZReader.read(file);
            return s == null
                    ? "Missing file on computer"
                    : s;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this
                || (obj instanceof SWGMacros
                    && file.getParentFile().getName().equals(((SWGMacros)
                            obj).file.getParentFile().getName()));
    }

    /**
     * Returns {@code true} if the file exists at the current file system. This
     * method is defined by {@link File#exists()}.
     * 
     * @return {@code true} if a file exists
     */
    public boolean exists() {
        return file.exists();
    }

    @Override
    public String getDescription() {
        return toString();
    }

    public String getName() {
        return file.getName();
    }

    @Override
    public int hashCode() {
        return file.getParentFile().getName().hashCode();
    }

    @Override
    public String toString() {
        return String.format("SWGMacros[%s]", file);
    }
}
