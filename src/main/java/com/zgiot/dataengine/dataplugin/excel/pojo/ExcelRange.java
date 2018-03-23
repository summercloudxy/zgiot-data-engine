package com.zgiot.dataengine.dataplugin.excel.pojo;

/**
 * Excel区域
 */
public class ExcelRange {
	private Integer id;
	private String name;
	private Integer system;
	//起点位置(x,y)
	private Integer startX;
	private Integer startY;
	//区域行数
	private Integer row;
	//读取方式
	private Integer readMode;
	//各个属性所在单元格距离起点（startX）间隔
	private Integer timeGap;
	private Integer deviceIdGap;
	private Integer targetGap;
	private Integer aadGap;
	private Integer mtGap;
	private Integer stadGap;
	private Integer qarGap;

	private Integer positive1Point45Gap;
	private Integer negative1Point45Gap;
	private Integer positive1Point8Gap;
	private Integer negative1Point8Gap;
	private Integer onePoint45To1Point8Gap;
	private Integer positive50mmGap;
	private Integer negative50mmGap;



	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getSystem() {
		return system;
	}

	public void setSystem(Integer system) {
		this.system = system;
	}

	public Integer getStartX() {
		return startX;
	}

	public void setStartX(Integer startX) {
		this.startX = startX;
	}

	public Integer getStartY() {
		return startY;
	}

	public void setStartY(Integer startY) {
		this.startY = startY;
	}

	public Integer getRow() {
		return row;
	}

	public void setRow(Integer row) {
		this.row = row;
	}

	public Integer getReadMode() {
		return readMode;
	}

	public void setReadMode(Integer readMode) {
		this.readMode = readMode;
	}

	public Integer getTimeGap() {
		return timeGap;
	}

	public void setTimeGap(Integer timeGap) {
		this.timeGap = timeGap;
	}

	public Integer getDeviceIdGap() {
		return deviceIdGap;
	}

	public void setDeviceIdGap(Integer deviceIdGap) {
		this.deviceIdGap = deviceIdGap;
	}

	public Integer getTargetGap() {
		return targetGap;
	}

	public void setTargetGap(Integer targetGap) {
		this.targetGap = targetGap;
	}

	public Integer getAadGap() {
		return aadGap;
	}

	public void setAadGap(Integer aadGap) {
		this.aadGap = aadGap;
	}

	public Integer getMtGap() {
		return mtGap;
	}

	public void setMtGap(Integer mtGap) {
		this.mtGap = mtGap;
	}

	public Integer getStadGap() {
		return stadGap;
	}

	public void setStadGap(Integer stadGap) {
		this.stadGap = stadGap;
	}

	public Integer getQarGap() {
		return qarGap;
	}

	public void setQarGap(Integer qarGap) {
		this.qarGap = qarGap;
	}

	public Integer getPositive1Point45Gap() {
		return positive1Point45Gap;
	}

	public void setPositive1Point45Gap(Integer positive1Point45Gap) {
		this.positive1Point45Gap = positive1Point45Gap;
	}

	public Integer getNegative1Point45Gap() {
		return negative1Point45Gap;
	}

	public void setNegative1Point45Gap(Integer negative1Point45Gap) {
		this.negative1Point45Gap = negative1Point45Gap;
	}

	public Integer getPositive1Point8Gap() {
		return positive1Point8Gap;
	}

	public void setPositive1Point8Gap(Integer positive1Point8Gap) {
		this.positive1Point8Gap = positive1Point8Gap;
	}

	public Integer getNegative1Point8Gap() {
		return negative1Point8Gap;
	}

	public void setNegative1Point8Gap(Integer negative1Point8Gap) {
		this.negative1Point8Gap = negative1Point8Gap;
	}

	public Integer getOnePoint45To1Point8Gap() {
		return onePoint45To1Point8Gap;
	}

	public void setOnePoint45To1Point8Gap(Integer onePoint45To1Point8Gap) {
		this.onePoint45To1Point8Gap = onePoint45To1Point8Gap;
	}

	public Integer getPositive50mmGap() {
		return positive50mmGap;
	}

	public void setPositive50mmGap(Integer positive50mmGap) {
		this.positive50mmGap = positive50mmGap;
	}

	public Integer getNegative50mmGap() {
		return negative50mmGap;
	}

	public void setNegative50mmGap(Integer negative50mmGap) {
		this.negative50mmGap = negative50mmGap;
	}

	@Override
	public String toString() {
		return "ExcelRange{" +
				"id=" + id +
				", name='" + name + '\'' +
				", system=" + system +
				", startX=" + startX +
				", startY=" + startY +
				", row=" + row +
				'}';
	}
}
