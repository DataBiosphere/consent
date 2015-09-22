package org.genomebridge.consent.http.service;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

public class UseRestrictionConfig {

    @NotNull
    private String methods;

    @NotNull
    private String population;

    @NotNull
    private String men;

    @NotNull
    private String women;

    @NotNull
    private String profit;

    @NotNull
    private String nonProfit;

    @NotNull
    private String pediatric;

    private Map<String, String> values = new HashMap<>();

    public String getMethods() {
        return methods;
    }

    public void setMethods(String methods) {
        this.methods = methods;
        values.put("methods", methods);
    }

    public String getPopulation() {
        return population;
    }

    public void setPopulation(String population) {
        this.population = population;
        values.put("population", population);
    }

    public String getMen() {
        return men;
    }

    public void setMen(String men) {
        this.men = men;
        values.put("men", men);
    }

    public String getWomen() {
        return women;
    }

    public void setWomen(String women) {
        this.women = women;
        values.put("women", women);
    }

    public String getProfit() {
        return profit;
    }

    public void setProfit(String profit) {
        this.profit = profit;
        values.put("profit", profit);
    }

    public String getNonProfit() {
        return nonProfit;
    }

    public void setNonProfit(String nonProfit) {
        this.nonProfit = nonProfit;
        values.put("nonProfit", profit);
    }

    public String getPediatric() {
        return pediatric;
    }

    public void setPediatric(String pediatric) {
        this.pediatric = pediatric;
        values.put("pediatric", pediatric);
    }

    public String getValueByName(String key){
        return this.values.getOrDefault(key, "Nonexistent Ontology");
    }
}
