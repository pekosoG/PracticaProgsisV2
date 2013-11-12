package com;

public class Compara_inst{
	public String Identificador="";
	public String REGEX="";
	public int min=0;
	public int max=0;
	public String Descrip="";
	public Compara_inst(String id,String regx,int min,int max,String dec) {
		this.Identificador=id;
		this.REGEX=regx;
		this.min=min;
		this.max=max;
		this.Descrip=dec;
	}
}