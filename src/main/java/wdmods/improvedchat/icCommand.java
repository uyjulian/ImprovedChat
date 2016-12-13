package wdmods.improvedchat;

/**
 *
 * @author wd1966
 */
public abstract class icCommand
{
    public String usage;
    public String desc;
    public String success;

    public icCommand(String description, String usageText, String successMessage)
    {
        desc = description;
        usage = usageText;
        success = successMessage;
    }

    public abstract boolean process(String[] args);
}
