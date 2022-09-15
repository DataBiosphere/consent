package org.broadinstitute.consent.http.models.dataset_registration_v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;

import javax.annotation.processing.Generated;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "fileType",
    "functionalEquivalence",
    "numberOfParticipants"
})
@Generated("jsonschema2pojo")
public class FileTypeObject {

    /**
     * File Type
     * (Required)
     *
     */
    @JsonProperty("fileType")
    @JsonPropertyDescription("File Type")
    private FileTypeObject.FileType fileType;
    /**
     * Functional Equivalence
     * (Required)
     *
     */
    @JsonProperty("functionalEquivalence")
    @JsonPropertyDescription("Functional Equivalence")
    private String functionalEquivalence;
    /**
     * # of Participants
     * (Required)
     *
     */
    @JsonProperty("numberOfParticipants")
    @JsonPropertyDescription("# of Participants")
    private Integer numberOfParticipants;

    /**
     * File Type
     * (Required)
     *
     */
    @JsonProperty("fileType")
    public FileTypeObject.FileType getFileType() {
        return fileType;
    }

    /**
     * File Type
     * (Required)
     *
     */
    @JsonProperty("fileType")
    public void setFileType(FileTypeObject.FileType fileType) {
        this.fileType = fileType;
    }

    /**
     * Functional Equivalence
     * (Required)
     *
     */
    @JsonProperty("functionalEquivalence")
    public String getFunctionalEquivalence() {
        return functionalEquivalence;
    }

    /**
     * Functional Equivalence
     * (Required)
     *
     */
    @JsonProperty("functionalEquivalence")
    public void setFunctionalEquivalence(String functionalEquivalence) {
        this.functionalEquivalence = functionalEquivalence;
    }

    /**
     * # of Participants
     * (Required)
     *
     */
    @JsonProperty("numberOfParticipants")
    public Integer getNumberOfParticipants() {
        return numberOfParticipants;
    }

    /**
     * # of Participants
     * (Required)
     *
     */
    @JsonProperty("numberOfParticipants")
    public void setNumberOfParticipants(Integer numberOfParticipants) {
        this.numberOfParticipants = numberOfParticipants;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(FileTypeObject.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("fileType");
        sb.append('=');
        sb.append(((this.fileType == null)?"<null>":this.fileType));
        sb.append(',');
        sb.append("functionalEquivalence");
        sb.append('=');
        sb.append(((this.functionalEquivalence == null)?"<null>":this.functionalEquivalence));
        sb.append(',');
        sb.append("numberOfParticipants");
        sb.append('=');
        sb.append(((this.numberOfParticipants == null)?"<null>":this.numberOfParticipants));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.numberOfParticipants == null)? 0 :this.numberOfParticipants.hashCode()));
        result = ((result* 31)+((this.functionalEquivalence == null)? 0 :this.functionalEquivalence.hashCode()));
        result = ((result* 31)+((this.fileType == null)? 0 :this.fileType.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof FileTypeObject) == false) {
            return false;
        }
        FileTypeObject rhs = ((FileTypeObject) other);
        return ((((this.numberOfParticipants == rhs.numberOfParticipants)||((this.numberOfParticipants!= null)&&this.numberOfParticipants.equals(rhs.numberOfParticipants)))&&((this.functionalEquivalence == rhs.functionalEquivalence)||((this.functionalEquivalence!= null)&&this.functionalEquivalence.equals(rhs.functionalEquivalence))))&&((this.fileType == rhs.fileType)||((this.fileType!= null)&&this.fileType.equals(rhs.fileType))));
    }


    /**
     * File Type
     *
     */
    @Generated("jsonschema2pojo")
    public enum FileType {

        ARRAYS("Arrays"),
        GENOME("Genome"),
        EXOME("Exome"),
        SURVEY("Survey"),
        PHENOTYPE("Phenotype");
        private final String value;
        private final static Map<String, FileTypeObject.FileType> CONSTANTS = new HashMap<String, FileTypeObject.FileType>();

        static {
            for (FileTypeObject.FileType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        FileType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static FileTypeObject.FileType fromValue(String value) {
            FileTypeObject.FileType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
