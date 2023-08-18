package com.vega.service.api.common;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.map.annotate.JsonSerialize;


@XmlRootElement(name = "Result")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class StoreResult {
	private int errorCode = -1;
    private StoreSubProfileInfo subProfile = null;

    /**
     * @return the errorCode
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * @param errorCode the errorCode to set
     */
    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * @return the subProfile
     */
    public StoreSubProfileInfo getSubProfile() {
        return subProfile;
    }

    /**
     * @param subProfile the subProfile to set
     */
    public void setSubProfile(StoreSubProfileInfo subProfile) {
        this.subProfile = subProfile;
    }
}
