package wdmods.improvedchat;

/**
 *
 * @author wd1966
 */
public abstract class icChannel
{
    private Character prefix;
    private boolean fallthrough = false;

    public abstract boolean process(String action, String[] args);

    protected icChannel(Character prefix, boolean fallthrough)
    {
        this.prefix = prefix;
        this.fallthrough = fallthrough;
    }

    public void setFallthrough(boolean fallthrough)
    {
        this.fallthrough = fallthrough;
    }

    public boolean isFallthrough()
    {
        return fallthrough;
    }

    public Character getPrefix()
    {
        return prefix;
    }
}
