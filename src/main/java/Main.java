import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import exceptions.ChebiException;
import uk.ac.ebi.chebi.webapps.chebiWS.client.ChebiWebServiceClient;
import uk.ac.ebi.chebi.webapps.chebiWS.model.ChebiWebServiceFault_Exception;
import uk.ac.ebi.chebi.webapps.chebiWS.model.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.Random;

public class Main {

    private List<Identifier> listIdent;
    private static ChebiWebServiceClient client = new ChebiWebServiceClient();

    /*public static void main(String[] args) throws ClassNotFoundException, SQLException {
        //String filePath = "/home/maria/repos/compound_identifiers.csv";
        String filePath = "C:\\Users\\blanc\\OneDrive\\Escritorio\\compound_identifiers.csv";

        List<Identifier> identifierList = readCSV(filePath);

        List<Integer> chebiNumbers = new ArrayList<>();
        // Print the data to verify
        for (Identifier identifier : identifierList) {
            //System.out.println(identifier);
            int compoundID = identifier.getCompoundID();
            try {
                Integer chebi = getChebiFromIdentifiers(identifier);
                    String sql = "insert ignore into compounds_chebi (compound_id, chebi_id) values ("+compoundID+", "+chebi+");";

                    writeToFile(sql, "outputFile.txt");
            } catch (ChebiException e) {
                //search in db
                System.out.println("----Database search...");
                String url = "jdbc:mysql://localhost:3306/chebi";
                String user = "root";
                String password = "root";

                Class.forName("com.mysql.cj.jdbc.Driver");
                Connection conn = DriverManager.getConnection(url, user, password);

                Statement stmt = conn.createStatement();
                String sql1 = "SELECT compound_id FROM structures WHERE structure LIKE ?";
                PreparedStatement pstmt = conn.prepareStatement(sql1);
                pstmt.setString(1, identifier.getInchi());

                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    Integer chebi = rs.getInt("compound_id");
                    String sql = "insert ignore into compounds_chebi (compound_id, chebi_id) values ("+compoundID+", "+chebi+");";
                    writeToFile(sql, "outputFile.txt");
                }
                rs.close();
                pstmt.close();
                conn.close();
                //System.out.println("Error processing identifier: " + identifier + ". " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Error processing due to network??: " + identifier + ". " + e.getMessage());
            }
        }
        //System.out.println(chebiNumbers);
    }*/

    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        //String filePath = "/home/maria/repos/compound_identifiers.csv";
        String filePath = "C:\\Users\\blanc\\OneDrive\\Escritorio\\compound_identifiers.csv";

        List<Identifier> identifierList = readCSV(filePath);

        List<Integer> chebiNumbers = new ArrayList<>();

        //Connection to compounds DDBB
        String jdbcURL = "jdbc:mysql://localhost:3306/compounds";
        String user = "root";
        String password = "root";
        //Connection to chebi DDBB
        String url = "jdbc:mysql://localhost:3306/chebi";
        user = "root";
        password = "root";
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection conn = DriverManager.getConnection(url, user, password);

        // Print the data to verify
        for (Identifier identifier : identifierList) {
            //System.out.println(identifier);
            int compoundID = identifier.getCompoundID();
            try {
                try (Connection connection = DriverManager.getConnection(jdbcURL, user, password)) {
                    String sql = "SELECT * FROM compounds_chebi WHERE compound_id LIKE ?";

                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setString(1, String.valueOf(compoundID));

                        try (ResultSet resultSet = statement.executeQuery()) {
                            if (resultSet.next()) { //It's already in the DDBB
                                System.out.println("----It's in the compounds_chebi DDBB...");
                                resultSet.close();
                                statement.close();
                                connection.close();
                                //break;
                            } else { //It's NOT in the DDBB
                                Statement stmt = conn.createStatement();
                                String sql1 = "SELECT compound_id FROM structures WHERE structure LIKE ?";
                                PreparedStatement pstmt = conn.prepareStatement(sql1);
                                pstmt.setString(1, identifier.getInchi());

                                ResultSet rs = pstmt.executeQuery();
                                if (rs.next()) { //It's in the chebi DDBB
                                    System.out.println("----Database search...");
                                    Integer chebi = rs.getInt("compound_id");
                                    sql = "insert ignore into compounds_chebi (compound_id, chebi_id) values ("+compoundID+", "+chebi+");";
                                    writeToFile(sql, "outputFile.txt");
                                } else { //It's NOT in the chebi DDBB
                                    Integer chebi = getChebiFromIdentifiers(identifier);
                                    sql = "insert ignore into compounds_chebi (compound_id, chebi_id) values ("+compoundID+", "+chebi+");";
                                    writeToFile(sql, "outputFile.txt");
                                }
                                rs.close();
                                pstmt.close();
                                conn.close();
                            }
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                //sleep between searches
                Thread.sleep((new Random().nextInt(3) + 1) * 1000);
            } catch (Exception e) {
                System.out.println("Error processing due to network??: " + identifier + ". " + e.getMessage());
            }
        }
    }

    public static void writeToFile(String content, String outputFilePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath, true))) {
            writer.write(content);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    public static List<Integer> extractNumbers(List<String> chebiStrings) {
        List<Integer> numbers = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\d+");

        for (String chebiString : chebiStrings) {
            Matcher matcher = pattern.matcher(chebiString);
            if (matcher.find()) {
                numbers.add(Integer.parseInt(matcher.group()));
            }
        }

        return numbers;
    }

    /**
     *
     * @param identifiers
     * @return chebId if it was found.
     * @throws exceptions.ChebiException
     */
    public static Integer getChebiFromIdentifiers(Identifier identifiers) throws ChebiException, SQLException, ClassNotFoundException {
        Integer chebiIdResult = null;
        String smiles = identifiers.getSmiles();
        String inchi_key = identifiers.getInchi_key();
        String inchi = identifiers.getInchi();
        if (smiles == null || inchi_key == null) {
            throw new ChebiException("Wrong identifier sent to chebi");
        }
        try {
            LiteEntityList querySMILESResult = client.getStructureSearch(smiles, StructureType.SMILES, StructureSearchCategory.IDENTITY, 10, 0.90F);
            List<LiteEntity> querySMILESList = querySMILESResult.getListElement();
            for (LiteEntity chebiEntity : querySMILESList) {
                String chebId = chebiEntity.getChebiId();
                Entity fullEntity;
                fullEntity = client.getCompleteEntity(chebId);
                String inchi_key_from_chebi = fullEntity.getInchiKey();
                String inchi__from_chebi = fullEntity.getInchi();
                if ((inchi_key_from_chebi != null && inchi_key_from_chebi.equals(inchi_key)) || (inchi__from_chebi != null && inchi__from_chebi.equals(inchi))) {
                    return ChebiDatabase.getChebiNumber(chebId);
                }
            }
        } catch (ChebiWebServiceFault_Exception ex) {

            System.out.println("CHEBI STRUCTURE WRONG: " + identifiers);
            Logger.getLogger(ChebiDatabase.class.getName()).log(Level.SEVERE, null, ex);
        }
        throw new ChebiException("ChebId not found");
    }

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

    public static List<Identifier> readCSV(String filePath) {
        List<Identifier> identifierList = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (values.length == 4) {
                    int compoundID = Integer.parseInt(values[0].replace("\"", ""));
                    String inchi = values[1].replace("\"", "");
                    String inchiKey = values[2].replace("\"", "");
                    String smiles = values[3].replace("\"", "");
                    identifierList.add(new Identifier(compoundID, inchi, inchiKey, smiles));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return identifierList;
    }

    public static Integer getChebiNumber(String chebiId) {
        return Integer.parseInt(chebiId.replaceAll("CHEBI:", ""));
    }

}