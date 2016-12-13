package wdmods.improvedchat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Translation class using google API, doesn't seem to be used any more
 *
 * @author wd1966
 * @author Adam Mummery-Smith
 */
public class Translator
{
    private static List<String> languages = new ArrayList<String>();
    private static URL url;

    public static boolean isValidLanguage(String languageName) // previously validLanguage
    {
//        Iterator<String> langIterator = languages.iterator();
//        String language;
//
//        do
//        {
//            if (!langIterator.hasNext())
//            {
//                return false;
//            }
//
//            language = langIterator.next();
//        }
//        while (!language.equals(languageName));

        return Translator.languages.contains(languageName);
    }

    private static String inputStreamToString(InputStream inputStream)
    {
        StringBuilder output = new StringBuilder();

        try
        {
            if (inputStream != null)
            {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                String line;

                while ((line = reader.readLine()) != null)
                    output.append(line).append('\n');
            }
        }
        catch (Exception ex) {}

        return output.toString();
    }

    public static String translate(String fromLanguage, String toLanguage, String text)
    {
        if (Translator.url == null)
            return text;

        String translated = text;

		try
		{
		    HttpURLConnection http = (HttpURLConnection)Translator.url.openConnection();
		    http.setRequestMethod("GET");
		    http.setDoOutput(true);

		    PrintWriter postWriter = new PrintWriter(http.getOutputStream());
		    postWriter.write("v=2.0&langpair=" + fromLanguage + "%7C" + toLanguage + "&q=" + URLEncoder.encode(text, "UTF-8"));
		    postWriter.flush();

		    try
		    {
		        String response = Translator.inputStreamToString(http.getInputStream());

		        if (response.contains("\"responseStatus\": 200"))
		        {
		            int startPos = response.indexOf("\"translatedText\":\"") + 18;
		            translated = response.substring(startPos, response.indexOf(34, startPos)).replaceAll("\\\\u0026#39;", "\'");
		        }
		    }
		    finally
		    {
		        http.getInputStream().close();

		        if (http.getErrorStream() != null)
		            http.getErrorStream().close();
		    }
		}
		catch (Exception ex) {}

		return translated;
    }

    static
    {
        Translator.languages.add("");
        Translator.languages.add("af");
        Translator.languages.add("sq");
        Translator.languages.add("am");
        Translator.languages.add("ar");
        Translator.languages.add("hy");
        Translator.languages.add("az");
        Translator.languages.add("eu");
        Translator.languages.add("be");
        Translator.languages.add("bn");
        Translator.languages.add("bh");
        Translator.languages.add("bg");
        Translator.languages.add("my");
        Translator.languages.add("ca");
        Translator.languages.add("chr");
        Translator.languages.add("zh");
        Translator.languages.add("zh-CN");
        Translator.languages.add("zh-TW");
        Translator.languages.add("hr");
        Translator.languages.add("cs");
        Translator.languages.add("da");
        Translator.languages.add("dv");
        Translator.languages.add("nl");
        Translator.languages.add("en");
        Translator.languages.add("eo");
        Translator.languages.add("et");
        Translator.languages.add("tl");
        Translator.languages.add("fi");
        Translator.languages.add("fr");
        Translator.languages.add("gl");
        Translator.languages.add("ka");
        Translator.languages.add("de");
        Translator.languages.add("el");
        Translator.languages.add("gn");
        Translator.languages.add("gu");
        Translator.languages.add("iw");
        Translator.languages.add("hi");
        Translator.languages.add("hu");
        Translator.languages.add("is");
        Translator.languages.add("id");
        Translator.languages.add("iu");
        Translator.languages.add("ga");
        Translator.languages.add("it");
        Translator.languages.add("ja");
        Translator.languages.add("kn");
        Translator.languages.add("kk");
        Translator.languages.add("km");
        Translator.languages.add("ko");
        Translator.languages.add("ku");
        Translator.languages.add("ky");
        Translator.languages.add("lo");
        Translator.languages.add("lv");
        Translator.languages.add("lt");
        Translator.languages.add("mk");
        Translator.languages.add("ms");
        Translator.languages.add("ml");
        Translator.languages.add("mt");
        Translator.languages.add("mr");
        Translator.languages.add("mn");
        Translator.languages.add("ne");
        Translator.languages.add("no");
        Translator.languages.add("or");
        Translator.languages.add("ps");
        Translator.languages.add("fa");
        Translator.languages.add("pl");
        Translator.languages.add("pt");
        Translator.languages.add("pa");
        Translator.languages.add("ro");
        Translator.languages.add("ru");
        Translator.languages.add("sa");
        Translator.languages.add("sr");
        Translator.languages.add("sd");
        Translator.languages.add("si");
        Translator.languages.add("sk");
        Translator.languages.add("sl");
        Translator.languages.add("es");
        Translator.languages.add("sw");
        Translator.languages.add("sv");
        Translator.languages.add("tg");
        Translator.languages.add("ta");
        Translator.languages.add("tl");
        Translator.languages.add("te");
        Translator.languages.add("th");
        Translator.languages.add("bo");
        Translator.languages.add("tr");
        Translator.languages.add("uk");
        Translator.languages.add("ur");
        Translator.languages.add("uz");
        Translator.languages.add("ug");
        Translator.languages.add("vi");
        Translator.languages.add("cy");
        Translator.languages.add("yi");

        try
        {
            Translator.url = new URL("http://ajax.googleapis.com/ajax/services/language/translate");
        }
        catch (MalformedURLException ex)
        {
            Translator.url = null;
        }
    }
}
