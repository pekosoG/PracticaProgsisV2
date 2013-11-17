package com;

public class LineaASM {
	
	private String etiqueta="";
	private String instruccion="";
	private String operando="";
	private String problema="";
	private ResultadoTabop resultTabop=null;
	private String result="";
	private String conloc="";
	private String codMaq="";
	
	
	public LineaASM(String etiqueta, String instruccion, String operando) {
		if(etiqueta.length()>1)
			this.etiqueta = etiqueta;
		if(instruccion.length()>1)
			this.instruccion = instruccion;
		if(operando.length()>1)
			this.operando = operando;
	}
	
	public LineaASM() {
		//Constructor vacío
	}

	public void setConloc(String conloc) {
		this.conloc = conloc;
	}
	
	public String getConloc() {
		return this.conloc;
	}

	public String getEtiqueta() {
		return etiqueta;
	}

	public String getInstruccion() {
		return instruccion;
	}

	public String getOperando() {
		return operando;
	}

	public void setCodMaq(String codMaq) {
		this.codMaq=codMaq;
	}
	
	public String getCodMaq() {
		return this.codMaq;
	}
	
	public void setEtiqueta(String etiqueta) {
		this.etiqueta = etiqueta;
	}

	public void setInstruccion(String instruccion) {
		this.instruccion = instruccion;
	}

	public void setOperando(String operando) {
		this.operando = operando;
	}
	
	public void setProblema(String problema) {
		if(problema.length()>0)
			this.problema+=problema;
		else
			this.problema="";
	}
	public String getProblema() {
		return this.problema;
	}
	
	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result += result;
	}
	
	public void setResTabop(ResultadoTabop res) {
		this.resultTabop=res;
	}
	public ResultadoTabop getResTabop() {
		return this.resultTabop;
	}

	public String toString() {
		if(problema.length()>0)
			return "Conloc: "+this.conloc+"Et:"+this.etiqueta+" - Inst: "+this.instruccion+" - Op: "+this.operando+" "+" Problema: "+this.problema+"\n---";
		else if(result.length()>0)
			return "Et:"+this.etiqueta+" - Inst: "+this.instruccion+" - Op: "+this.operando+"\nTabop "+result+":\n\t "+resultTabop+"---";
		else
			return "Et:"+this.etiqueta+" - Inst: "+this.instruccion+" - Op: "+this.operando+"\n---";
	}
}

class Comentario extends LineaASM{
	private String comentario;
	
	public Comentario(String cadena) {
		super();
		if(cadena.length()>0)
			this.comentario=cadena.substring(1,cadena.length());
	}

	public String getComentario() {
		return comentario;
	}

	public void setComentario(String comentario) {
		this.comentario = comentario;
	}
	
	public String toString() {
		return "Comentario: "+this.comentario;
	}
	
}
