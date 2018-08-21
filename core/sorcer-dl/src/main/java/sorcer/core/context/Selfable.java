package sorcer.core.context;

import sorcer.service.Arg;

public interface Selfable extends Arg {

    public boolean isSelf();

    public void setSelf(boolean self);

}
