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
}
