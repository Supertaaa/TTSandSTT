package com.vega.service.api.response;
import com.vega.service.api.object.TopTenInfo;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.map.annotate.JsonSerialize;

@XmlRootElement(name = "ResultTopten")
@JsonSerialize(include = JsonSerialize.Inclusion.ALWAYS)
public class ResultTopten extends ResultAbstract {

	private int countListendInNDays;
	private List<TopTenInfo> userListTopten;
	private int total;

	public int getCountListendInNDays() {
		return countListendInNDays;
	}

	public void setCountListendInNDays(int countListendInNDays) {
		this.countListendInNDays = countListendInNDays;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public List<TopTenInfo> getUserListTopten() {
		return userListTopten;
	}

	public void setUserListTopten(List<TopTenInfo> userListTopten) {
		this.userListTopten = userListTopten;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ResultTopten, total = " + total + ", countListendInNDays:" + countListendInNDays;
	}
}
