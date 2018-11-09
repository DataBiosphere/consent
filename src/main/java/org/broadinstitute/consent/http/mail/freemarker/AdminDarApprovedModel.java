package org.broadinstitute.consent.http.mail.freemarker;

import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AdminDarApprovedModel {

    private String userName;

    private String referenceId;

    private List<DatasetOwnerListModel> dol;

    private String serverUrl;

    public AdminDarApprovedModel(String user, String referenceId, Map<DACUser, List<DataSet>> dataOwnersDataSets, String serverUrl) {
        this.userName = user;
        this.referenceId = referenceId;
        this.serverUrl = serverUrl;
        this.dol = ownersMapToTemplateList(dataOwnersDataSets);
    }

    public String getUserName() {
        return userName;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public List<DatasetOwnerListModel> getDol() {
        return dol;
    }

    private List<DatasetOwnerListModel> ownersMapToTemplateList(Map<DACUser, List<DataSet>> dataOwnersDataSets){
        List<DatasetOwnerListModel> templateList = new ArrayList<>();
        Set<DACUser> owners = dataOwnersDataSets.keySet();
        for(DACUser owner: owners){
            templateList.addAll(dataOwnersDataSets.get(owner).stream().map(dataSet -> new DatasetOwnerListModel(owner.getDisplayName(), dataSet.getName(), dataSet.getObjectId())).collect(Collectors.toList()));
        }
        return templateList;
    }
}