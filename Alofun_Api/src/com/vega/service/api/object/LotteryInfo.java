package com.vega.service.api.object;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.map.annotate.JsonSerialize;

@XmlRootElement(name = "LotteryInfo")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class LotteryInfo{

    private int id;
    private int region;
    private int provinceId = -1;
    private String publishDate;
    private int lotNo = -1;
    private int ord;
    private String val;
    private String postfixVal;
    private String prefixVal;
    private String source;
    private String updatedDate;
    private int total;
     
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLotNo() {
        return lotNo;
    }

    public void setLotNo(int lotNo) {
        this.lotNo = lotNo;
    }

    public String getPostfixVal() {
        return postfixVal;
    }

    public void setPostfixVal(String postfixVal) {
        this.postfixVal = postfixVal;
    }

    public String getPrefixVal() {
        return prefixVal;
    }

    public void setPrefixVal(String prefixVal) {
        this.prefixVal = prefixVal;
    }

    public int getProvinceId() {
        return provinceId;
    }

    public void setProvinceId(int provinceId) {
        this.provinceId = provinceId;
    }

    public String getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(String publishDate) {
        this.publishDate = publishDate;
    }

    public int getRegion() {
        return region;
    }

    public void setRegion(int region) {
        this.region = region;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(String updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getVal() {
        return val;
    }

    public void setVal(String val) {
        this.val = val;
    }

    /**
     * @return the ord
     */
    public int getOrd() {
        return ord;
    }

    /**
     * @param ord the ord to set
     */
    public void setOrd(int ord) {
        this.ord = ord;
    }

	/**
	 * @return the total
	 */
	public int getTotal() {
		return total;
	}

	/**
	 * @param total the total to set
	 */
	public void setTotal(int total) {
		this.total = total;
	}
   
}
