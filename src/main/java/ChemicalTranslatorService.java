
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ChemicalTranslatorService {



    public static List<String> getChemicalNames(String inchiKey) throws Exception {
        String urlString = "https://cts.fiehnlab.ucdavis.edu/rest/convert/InChIKey/ChEBI/" + inchiKey;
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("HTTP GET Request Failed with Error code : " + responseCode);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }

        in.close();
        conn.disconnect();

        Gson gson = new Gson();
        JsonArray jsonArray = gson.fromJson(content.toString(), JsonArray.class);

        List<String> chemicalNames = new ArrayList<>();
        if (jsonArray.size() > 0) {
            JsonObject jsonObject = jsonArray.get(0).getAsJsonObject();
            JsonArray resultArray = jsonObject.getAsJsonArray("results");
            for (JsonElement element : resultArray) {
                chemicalNames.add(element.getAsString());
            }
        }

        return chemicalNames;
    }

    public static void main(String[] args) {
        try {
            String inchiKey = "QNAYBMKLOCPYGJ-REOHCLBHSA-N";
            List<String> chemicalNames = getChemicalNames(inchiKey);
            System.out.println("Chemical Names: " + chemicalNames);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
