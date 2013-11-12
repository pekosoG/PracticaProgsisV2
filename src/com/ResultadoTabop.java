package com;

import java.io.FileNotFoundException;
import java.util.Formatter;

public class ResultadoTabop {
	
	/****inst-tiene_Operador-direcc-codmaq-bytesCalculados-bytesPorCalcular-total*/
	
	private String instrucc="";
	private boolean operando=false;
	private String direccionamiento="";
	private String codmaquina="";
	private String bytesCalculados="";
	private String bytesPorCalcular="";
	private int totalBytes=0;	
	
	public ResultadoTabop(String instrucc, boolean operando,
			String direccionamiento, String codmaquina, String bytesCalculados,
			String bytesPorCalcular, int totalBytes) {
		this.instrucc = instrucc;
		this.operando = operando;
		this.direccionamiento = direccionamiento;
		this.codmaquina = codmaquina;
		this.bytesCalculados = bytesCalculados;
		this.bytesPorCalcular = bytesPorCalcular;
		this.totalBytes = totalBytes;
	}
	
	public ResultadoTabop(String instrucc,String direcc,int totalBytes) {
		this.instrucc=instrucc;
		this.direccionamiento=direcc;
		this.totalBytes=totalBytes;
		if(!direcc.contains("ORG"))
			this.codmaquina+=new Formatter().format("%04x",totalBytes);
	}
	
	public String getInstrucc() {
		return instrucc;
	}

	public boolean isOperando() {
		return operando;
	}

	public String getDireccionamiento() {
		return direccionamiento;
	}

	public String getCodmaquina() {
		return codmaquina;
	}

	public String getBytesCalculados() {
		return bytesCalculados;
	}

	public String getBytesPorCalcular() {
		return bytesPorCalcular;
	}

	public int getTotalBytes() {
		return totalBytes;
	}
	
	public void setCodMaq(String codMaq) {
		this.codmaquina=codMaq;
	}

	public String toString() {
		return "CodMaq: "+codmaquina+"   Op: "+operando+" Tipo: "+direccionamiento+" B-Calcs: "+bytesCalculados+" B-X-Calc: "+bytesPorCalcular+" Total: "+totalBytes+"\n";
	}
	
}
