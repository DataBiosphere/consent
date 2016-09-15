package org.broadinstitute.consent.http.enumeration;


public enum RoleStatus {

    PENDING("pending", 0),
    APPROVED("approved", 1),
    REJECTED("rejected", 2);

    private String status;
    private Integer value;

    RoleStatus(String status, Integer value){
        this.value = value;
        this.status = status;
    }

    public static Integer getValueByStatus(String status){
     if(status != null){
         for (RoleStatus roleStatus : RoleStatus.values()) {
             if(roleStatus.status.equalsIgnoreCase(status)){
                 return roleStatus.value;
             }
         }
     }
     return null;
    }

    public static String getStatusByValue(Integer value){
        if(value != null){
            for (RoleStatus roleStatus : RoleStatus.values()) {
                if(roleStatus.value.equals(value)){
                    return roleStatus.status;
                }
            }
        }
        return null;
    }
}
