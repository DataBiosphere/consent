package org.broadinstitute.consent.http.configurations;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

public class UseRestrictionConfig {

    @NotNull
    private String methods;

    @NotNull
    private String population;

    @NotNull
    private String male;

    @NotNull
    private String female;

    @NotNull
    private String profit;

    @NotNull
    private String nonProfit;

    @NotNull
    private String boys;

    @NotNull
    private String girls;

    @NotNull
    private String pediatric;

    public UseRestrictionConfig() {
    }

    public UseRestrictionConfig(String methods, String population, String male, String female, String profit,
                                String nonProfit, String boys, String girls, String pediatric) {
        this.methods = methods;
        this.population = population;
        this.male = male;
        this.female = female;
        this.profit = profit;
        this.nonProfit = nonProfit;
        this.boys = boys;
        this.girls = girls;
        this.pediatric = pediatric;
    }

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

    public String getMale() {
        return male;
    }

    public void setMale(String male) {
        this.male = male;
        values.put("male", male);
    }

    public String getFemale() {
        return female;
    }

    public void setFemale(String female) {
        this.female = female;
        values.put("female", female);
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

    public String getBoys() {
        return boys;
    }

    public void setBoys(String boys) {
        this.boys = boys;
        values.put("boys", boys);
    }

    public String getGirls() {
        return girls;
    }

    public void setGirls(String girls) {
        this.girls = girls;
        values.put("girls", girls);
    }

    public void setPediatric(String pediatric) {
        this.pediatric = pediatric;
        values.put("pediatric", pediatric);
    }

    public String getValueByName(String key){
        return this.values.getOrDefault(key, "Nonexistent Ontology");
    }
}
