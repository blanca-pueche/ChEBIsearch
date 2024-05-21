

/*
 * ChebiDatabase.java
 *
 * Created on 07-nov-2019, 20:50:05
 *
 * Copyright(c) 2018 Ceu Mass Mediator All Rights Reserved.
 * This software is the proprietary information of Alberto Gil de la Fuente.
 *
 */
import exceptions.ChebiException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.ebi.chebi.webapps.chebiWS.client.ChebiWebServiceClient;
import uk.ac.ebi.chebi.webapps.chebiWS.model.ChebiWebServiceFault_Exception;
import uk.ac.ebi.chebi.webapps.chebiWS.model.*;

/**
 * Class to perform queries against the chebi Database to retrieve information
 * of compounds with the chebi Identifier
 *
 * @version $Revision: 5.1.1.1 $
 * @since Build 4.1.0.0 07-nov-2019
 *
 * @author Alberto Gil de la Fuente
 */
public abstract class ChebiDatabase {

    static ChebiWebServiceClient client = new ChebiWebServiceClient();

    public static String getInChIFromChebID(int chebId) throws ChebiWebServiceFault_Exception {
        try {

            Entity entity;
            entity = client.getCompleteEntity("CHEBI:" + Integer.toString(chebId));
            return entity.getInchi();
        } catch (ChebiWebServiceFault_Exception ex) {
            Logger.getLogger(ChebiDatabase.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public static Identifier getIdentfiersFromChebiId(int chebId) {
        try {

            Entity entity;
            entity = client.getCompleteEntity("CHEBI:" + Integer.toString(chebId));
            String inchi = entity.getInchi();
            String inchi_key = entity.getInchiKey();
            String canonicalSmiles = entity.getSmiles();
            Identifier identifier = new Identifier(inchi, inchi_key, canonicalSmiles);
            return identifier;

        } catch (ChebiWebServiceFault_Exception ex) {
            Logger.getLogger(ChebiDatabase.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
    }

    /**
     * Returns the ASCII name of the chebi ID
     *
     * @param chebId
     * @return
     * @throws ChebiWebServiceFault_Exception
     */
    public static String getAsciiName(int chebId) throws ChebiWebServiceFault_Exception {
        String ascii_name;
        try {
            Entity entity;
            entity = client.getCompleteEntity("CHEBI:" + Integer.toString(chebId));
            
            ascii_name = entity.getChebiAsciiName();
            if (ascii_name.equals("")) {
                return null;
            }

        } catch (ChebiWebServiceFault_Exception ex) {
            Logger.getLogger(ChebiDatabase.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        return ascii_name;
    }

    public static String getInChIKeyFromChebID(int chebId) throws ChebiWebServiceFault_Exception {
        try {
            Entity entity;
            entity = client.getCompleteEntity("CHEBI:" + Integer.toString(chebId));
            return entity.getInchiKey();
        } catch (ChebiWebServiceFault_Exception ex) {
            Logger.getLogger(ChebiDatabase.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public static String getFormulaFromChebID(int chebId) throws ChebiException, ChebiWebServiceFault_Exception {
        try {
            Entity entity;
            entity = client.getCompleteEntity("CHEBI:" + Integer.toString(chebId));
            List<DataItem> formulas = entity.getFormulae();
            if (formulas.size() == 1) {
                return formulas.get(0).getData();
            } else if (formulas.isEmpty()) {
                throw new ChebiException("Formula not available in chebi");
            } else {
                System.out.println("chebId: " + chebId);
                for (DataItem formula : formulas) {
                    System.out.println("DATA:" + formula.getData());
                    System.out.println("SOURCE:" + formula.getSource());
                    System.out.println("Type:" + formula.getType());
                }
                return formulas.get(0).getData();
            }
        } catch (ChebiWebServiceFault_Exception ex) {
            Logger.getLogger(ChebiDatabase.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        }
    }

    public static Double getMonoIsotopicMassFromChebID(int chebId) throws ChebiWebServiceFault_Exception {
        try {
            Entity entity;
            entity = client.getCompleteEntity("CHEBI:" + Integer.toString(chebId));
            Double mass = Double.parseDouble(entity.getMonoisotopicMass());
            return mass;
        } catch (ChebiWebServiceFault_Exception ex) {
            Logger.getLogger(ChebiDatabase.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public static String getHMDBLink(int chebId) {
        try {
            Entity entity;
            entity = client.getCompleteEntity("CHEBI:" + Integer.toString(chebId));
            List<DataItem> synonyms = entity.getDatabaseLinks();
            for (DataItem synonim : synonyms) {
                System.out.println("DATA:" + synonim.getData());
                System.out.println("SOURCE:" + synonim.getSource());
                System.out.println("Type:" + synonim.getType());
                if (synonim.getType().equals("HMDB accession")) {
                    return synonim.getData();
                }
            }
        } catch (ChebiWebServiceFault_Exception ex) {
            Logger.getLogger(ChebiDatabase.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        return null;
    }

    /**
     *
     * @param identifiers
     * @return chebId if it was found.
     * @throws exceptions.ChebiException
     */
    public static Integer getChebiFromIdentifiers(Identifier identifiers) throws ChebiException {
        Integer chebiIdResult = null;
        String smiles = identifiers.getSmiles();
        String inchi_key = identifiers.getInchi_key();
        String inchi = identifiers.getInchi();
        if (smiles == null || inchi_key == null) {
            throw new ChebiException("Wrong identifier sent to chebi");
        }
        try {
            LiteEntityList querySMILESResult = client.getStructureSearch(smiles, StructureType.SMILES, StructureSearchCategory.SIMILARITY, 100, 0.90F);
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

    public static Integer getChebiNumber(String chebiId) {
        return Integer.parseInt(chebiId.replaceAll("CHEBI:", ""));
    }
}
