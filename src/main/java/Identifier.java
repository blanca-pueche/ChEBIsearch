/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.Objects;

/**
 *
 * @author ceu
 */
public class Identifier {

    private int compoundID = 0;
    private final String inchi;
    private final String inchi_key;
    private final String smiles;

    public Identifier(int compoundID, String inchi, String inchi_key, String smiles) {
        this.compoundID = compoundID;
        this.inchi = inchi;
        this.inchi_key = inchi_key;
        this.smiles = smiles;
    }
    public Identifier(String inchi, String inchi_key, String smiles) {
        this.inchi = inchi;
        this.inchi_key = inchi_key;
        this.smiles = smiles;
    }

    public String getInchi() {
        return inchi;
    }

    public int getCompoundID() {
        return compoundID;
    }

    public String getInchi_key() {
        return inchi_key;
    }

    public String getSmiles() {
        return smiles;
    }

    @Override
    public String toString() {
        return "Identifier{" + "inchi=" + inchi + ", inchi_key=" + inchi_key + ", smiles=" + smiles + '}';
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.inchi_key);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Identifier other = (Identifier) obj;
        if (!Objects.equals(this.inchi_key, other.inchi_key)) {
            return false;
        }
        return true;
    }

}
