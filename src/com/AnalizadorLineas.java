package com;

import java.util.Formatter;
import java.util.Vector;

import javax.swing.JOptionPane;

public class AnalizadorLineas {
	
	private static String REGEX_Etiqueta="^[a-zA-Z]+([0-9]*[_]*[a-zA-Z]*)*";
	private static String REGEX_CodOP="^[a-zA-Z]+(([0-9]*[a-zA-Z]*)*[\\.]?([0-9]*[a-zA-Z]*)*)";
	private static String REGEX_Inmediato="[#][%$@#]*\\d*";
	private static String REGEX_Directo="[#%$@]*\\d*";
	private static String REGEX_Extendido=REGEX_Directo;
	private static String REGEX_xysp="([XxYy]|([sS][pP])|([pP][cC]))";
	private static String REGEX_Indizado="\\d*,"+REGEX_xysp+"$";
	private static String REGEX_Indizado_Indirecto="\\[\\d*,"+REGEX_xysp+"\\]";
	private static String REGEX_Indizado_PrePost="\\d*,(([-+]"+REGEX_xysp+")|("+REGEX_xysp+"[-+]))";
	private static String REGEX_Indizado_Acumulador="[AaBbDd],"+REGEX_xysp+"$";
	private static String REGEX_Indizado_Acumulador_Indirecto="\\[[AaBbDd],"+REGEX_xysp+"\\]$";
	private static String REGEX_Relativo="[^\\d*][a-zA-Z]+([0-9]*[_]*[a-zA-Z]*)*";
		
	public static boolean despuesEnd=false,huboEnd=false,huboORG=false;
	public static Vector<ResultadoTabop> tabop;
	public static Vector<Compara_inst> spects_ins;
	public static Vector<String> palTabsim;
	
	public static int elContloc=0;
	
	
	static {
		palTabsim=new Vector<String>();
		//Leemos el Tabop y lo cargamos en memoria
		tabop=new Vector<ResultadoTabop>();
		ManejaArchivo.leeTABOP("Tabop.txt", tabop);
		
		//Cargamos cada tipo de direccionamiento y sus caracteristica...
		spects_ins=new Vector<Compara_inst>();

		spects_ins.add(new Compara_inst("INH",".",0,0,"Inherente"));		
		
		spects_ins.add(new Compara_inst("IMM",REGEX_Inmediato,0,255,"Inmediato 8b"));
		
		spects_ins.add(new Compara_inst("IMM",REGEX_Inmediato,256,65535,"Inmediato 16b"));
		
		spects_ins.add(new Compara_inst("DIR",REGEX_Directo,0,255,"Directo"));
		
		spects_ins.add(new Compara_inst("EXT","("+REGEX_Etiqueta+")|("+REGEX_Extendido+")",0,65535,"Extendido"));
		
		spects_ins.add(new Compara_inst("IDX",REGEX_Indizado,-16,15,"Indizado 5b"));
		
		spects_ins.add(new Compara_inst("IDX1",REGEX_Indizado,-256,-17,"Indizado 9b"));
		
		spects_ins.add(new Compara_inst("IDX1",REGEX_Indizado,16,255,"Indizado 9b"));
		
		spects_ins.add(new Compara_inst("IDX2",REGEX_Indizado,256,65535,"Indizado 16b"));
		
		spects_ins.add(new Compara_inst("[IDX2]",REGEX_Indizado_Indirecto,0,65535,"Indizado 16b"));

		spects_ins.add(new Compara_inst("IDX",REGEX_Indizado_PrePost,1,8,"Indizado Pre-Post Decremento"));
		
		spects_ins.add(new Compara_inst("IDX",REGEX_Indizado_Acumulador,0,0,"Indizado Acumulador"));
		
		spects_ins.add(new Compara_inst("[D,IDX]",REGEX_Indizado_Acumulador_Indirecto,0,0,"Indizado Acumulador Indirecto"));

		spects_ins.add(new Compara_inst("REL",REGEX_Relativo,0,255,"Relativo 8b"));
		
		spects_ins.add(new Compara_inst("REL","("+REGEX_Etiqueta+")|("+REGEX_Relativo+")",255,65535,"Relativo 8b"));
		
	}
	
	public static Vector<LineaASM> procesaLineas(Vector<String> lineas){
		Vector<LineaASM> resultado=new Vector<LineaASM>();
		
		//Primera pasada!
		for(String tempLinea:lineas) {
			if(huboEnd)
				despuesEnd=true;
			if(tempLinea.charAt(0)==';')
				resultado.add(new Comentario(tempLinea));
			else
				resultado.add(analizaLinea(tempLinea));
			if(resultado.lastElement()==null) 
				resultado.remove(resultado.size()-1);
			else
				checaResultado(resultado.elementAt(resultado.size()-1));
		}
		
		//La segunda Vuleta!!!		
		for(LineaASM elementAt:resultado) {
			if(!(elementAt instanceof Comentario)&& elementAt.getProblema().length()==0) {
				if(elementAt.getOperando().length()>0&&!elementAt.getOperando().matches(REGEX_Etiqueta)) //para cambiar el OP a HEX
					elementAt.setOperando(Integer.toHexString(obtieneValor(elementAt.getOperando())));
				cambiaMaq(elementAt); //cambiamos el CodMaq...
			}else
				break;
		}
		if(despuesEnd)
			System.out.println("Warning: Existen mas lineas despues del END");
		
		LineaASM aux= new LineaASM();
		aux.setInstruccion("END");
		resultado.add(aux);
		ManejaArchivo.escribeTabsim(palTabsim,"TabSim.txt");
		
		return resultado;
	}

	private static LineaASM analizaLinea(String tempLinea) {
		String[]tokens=null;
		LineaASM aux=new LineaASM();
		try {
			if(tempLinea.trim().equalsIgnoreCase("end")) {
				huboEnd=true;
				return null;				
			}
			if(tempLinea.replaceAll("\\s+","").length()<=0)
				return null;
			
			//Ahora vamos a analizar la linea
			tempLinea=tempLinea.replaceAll("\\s+"," ");
			if(tempLinea.charAt(0)!=' ') { //Primero si tienen etiqueta
				/*Limpiamos*/
				tempLinea=tempLinea.trim();
				tokens=tempLinea.split("\\s+");
				if(tokens.length==0)
					return null;
				
				chekaEtiqueta(tokens[0],aux);
				try {
					chekaCodOp(tokens[1],aux);
					aux.setOperando("");
					if(tokens.length>2)
						aux.setOperando(tokens[2]);
					if(tokens.length>=4)
						aux.setProblema("Se encontraron mas elementos despues del operando");
				}catch(IndexOutOfBoundsException ee) {
					aux.setOperando(tokens[1]);
				}				
			}else {//Ahora si no tienen etiqueta
				/*Limpiamos*/
				tempLinea=tempLinea.trim();
				tokens=tempLinea.split("\\s+");				
				chekaCodOp(tokens[0],aux);
				aux.setOperando("");
				if(tokens.length>1)
					aux.setOperando(tokens[1]);
				if(tokens.length>=3)
					aux.setProblema("Se encontraron mas elementos despues del operando");
			}
			
		}catch(IndexOutOfBoundsException ee) {
			JOptionPane.showMessageDialog(null, "Hubo un problema al separar la linea ''"+tempLinea
					+"''\nNo tenia todos los elementos esperados");
		}catch(Exception e) {
			JOptionPane.showMessageDialog(null, "Problema desconocido en la linea ''"+tempLinea
					+"''\n"+e.getLocalizedMessage());
		}
		return aux;
	}

	private static void chekaCodOp(String codOp, LineaASM aux) {
		if(codOp.length()<=5 && codOp.matches(REGEX_CodOP))
			aux.setInstruccion(codOp);
		else
			aux.setProblema("CodOp Invalido: "+codOp+"; ");
	}

	private static void chekaEtiqueta(String etiqueta, LineaASM aux) {
		if(etiqueta.length()<=8 && etiqueta.matches(REGEX_Etiqueta))
			aux.setEtiqueta(etiqueta);
		else
			aux.setProblema("Etiqueta Invalida "+etiqueta+"; ");	
	}
	
	private static void checaResultado(LineaASM elementAt) {
		if(!elementAt.getProblema().equalsIgnoreCase("Se encontraron mas elementos despues del operando"))
		try {
			if(!esDirectiva(elementAt))
			for(ResultadoTabop resAux:tabop) {
				if(elementAt instanceof Comentario)
					return;
				if(elementAt.getInstruccion().equalsIgnoreCase(resAux.getInstrucc())) {
					if((resAux.isOperando() && elementAt.getOperando().length()>0) || (!resAux.isOperando() && elementAt.getOperando().length()<=0)){
						if(idenDir(elementAt,resAux)) { //aqui llamamos la rutina para ver que onda...							
							calculaConLoc(elementAt);
							identificaTabsim(elementAt);
							elementAt.setProblema("");
							return;
						}
						if(elementAt.getProblema().length()<=0)
							elementAt.setProblema("Error! No sirve tu linea feaaaaaa");
					}else {
						elementAt.setProblema("");
						elementAt.setProblema((elementAt.getResult().length()<=0)?"Error con el operando, Acepta Operando: "+resAux.isOperando()+", tiene operando: "+((elementAt.getOperando().length()>0)?elementAt.getOperando():"No"):"");
					}
				}else if(!elementAt.getProblema().contains("Error! 404 CodOP not Found") && elementAt.getProblema().length()<=0)
					elementAt.setProblema((elementAt.getResult().length()<=0)?"error 404 CodOP not Found":"");
			}
		}catch(Exception e) {
			if(e.getMessage()!=null)
				JOptionPane.showMessageDialog(null, "Error desconocido CHKRES "+e.getMessage());
		}
	}

	private static void cambiaMaq(LineaASM elementAt) {
		
		//Para el INH no hace falta hacer este desmadreeee!!!....
		
		if(elementAt.getResTabop().getDireccionamiento().equalsIgnoreCase("DIR")) {
			String auxMaq=elementAt.getResTabop().getCodmaquina();
			String auxOp=elementAt.getOperando();
			auxMaq=auxMaq.substring(0,auxMaq.length()-3);
			auxMaq+=auxOp;
			elementAt.setCodMaq(auxMaq);
		}
		
		if(elementAt.getResTabop().getDireccionamiento().equalsIgnoreCase("EXT")) {
			String auxMaq=elementAt.getResTabop().getCodmaquina();
			String auxOp=elementAt.getOperando();
			if(!auxOp.matches(REGEX_Etiqueta)) {
				auxOp=String.format("%4s", auxOp).replace(' ', '0');
				auxMaq=auxMaq.substring(0,auxMaq.length()-6);
				auxMaq+=auxOp;
				elementAt.setCodMaq(auxMaq);
			}else { //esto es en caso de que haya Etiqueta...
				for(String auxTab:palTabsim) {
					if(auxTab.contains(auxOp)) {
						String aux[]=auxTab.split("\\s");
						auxOp=aux[1];
						auxMaq=auxMaq.substring(0,auxMaq.length()-6);
						auxMaq+=auxOp;
						elementAt.setCodMaq(auxMaq);
					}
				}
			}
		}
		
		if(elementAt.getResTabop().getDireccionamiento().equalsIgnoreCase("IMM")) {
			int auxOP=obtieneValor("$"+elementAt.getOperando());
			String auxMaq=elementAt.getResTabop().getCodmaquina();
			String auxOp=elementAt.getOperando();
			if(auxOP>0&&auxOP<=255) {
				auxMaq=auxMaq.substring(0,auxMaq.length()-3);
				auxMaq+=auxOp;
				elementAt.setCodMaq(auxMaq);
			}else if(auxOP>255 && auxOP<=65535) {
				auxOp=String.format("%4s", auxOp).replace(' ', '0');
				auxMaq=auxMaq.substring(0,auxMaq.length()-6);
				auxMaq+=auxOp;
				elementAt.setCodMaq(auxMaq);
			}
		}
		
		//Falta hacer la rutina para que sean cadenas HEX bonitas...
	}

	private static void identificaTabsim(LineaASM elementAt) {
		boolean error=false;
		if(elementAt.getEtiqueta().length()>0) {
			for(String ee:palTabsim) {
				String a[]=ee.split("\\s");
				ee=a[0];
				if(ee.equals(elementAt.getEtiqueta()))
					error=true;
			}
			if(!error) {
				String aux=elementAt.getEtiqueta();
				aux+="\t"+elementAt.getConloc();
				palTabsim.add(aux);
			}else
				elementAt.setProblema("Etiqueta Duplicada!!");
		}
	}

	private static void calculaConLoc(LineaASM elementAt) {
		int Contaux=0;
		try {
			elementAt.setConloc(new Formatter().format("%04x", elContloc).toString().toUpperCase());
			Contaux=Integer.valueOf(elementAt.getResTabop().getBytesCalculados(),10);
			elContloc+=Contaux;
		}catch(Exception ee) {
			ee.printStackTrace();
		}
	}

	//Aqui es donde vamos a ver si la linea ASM concuerda con el leido del Tabop...
	private static boolean idenDir(LineaASM elementAt, ResultadoTabop resAux) {
		boolean result=false;
		
		for(Compara_inst comp_aux: spects_ins) {
			if(resAux.getDireccionamiento().equalsIgnoreCase(comp_aux.Identificador)) {
				try{
					String valor_String=(elementAt.getOperando().replace("[^\\d*#$%@]*"," ")).trim();
					if(valor_String.length()>0 && resAux.isOperando()) {
						if(!valor_String.matches(REGEX_Etiqueta)) {
							try {
								int valorElement=obtieneValor(valor_String);
								if(valorElement>=comp_aux.min&&valorElement<=comp_aux.max) {
									if(elementAt.getOperando().matches(comp_aux.REGEX)) {
										result=true;
										elementAt.setResult(comp_aux.Descrip);
										elementAt.setResTabop(resAux);
										break;
									}
								}else {elementAt.setProblema("");elementAt.setProblema("Error! Operando Invalido 1");}
							}catch(NumberFormatException NFE) {
								String aux[]=valor_String.split(",");
								int valorElement=obtieneValor(aux[0]);
								if(valorElement>=comp_aux.min&&valorElement<=comp_aux.max) {
									if(elementAt.getOperando().matches(comp_aux.REGEX)) {
										result=true;
										elementAt.setResult(comp_aux.Descrip);
										elementAt.setResTabop(resAux);
										break;
									}
								}else { elementAt.setProblema("");elementAt.setProblema("Error! Operando Invalido 2");}
							}
						}else{
							if(elementAt.getOperando().matches(comp_aux.REGEX)) {
								result=true;
								elementAt.setResult(comp_aux.Descrip);
								elementAt.setResTabop(resAux);
								break;
							}else {elementAt.setProblema(""); elementAt.setProblema("Error! Operando Invalido 3");}
						}
					}
					else {
						result=true;
						elementAt.setResult(comp_aux.Descrip);
						elementAt.setResTabop(resAux);
						break;
					}						
				}catch(Exception e) {
					//System.out.println("Error en idenDir "+e);
					elementAt.setProblema("");
					elementAt.setProblema("Operando Invalido, "+e);
					return false;
				}
			}
		}
		
		return result;
	}

	private static int obtieneValor(String valor_String) {
		int result=0;
		String aux;
		char ini=valor_String.charAt(0);
		char sec='_';
		try {
		sec=valor_String.charAt(1);
		}catch(java.lang.StringIndexOutOfBoundsException ee) {
			//ee.printStackTrace();
		}
		
		if(ini=='#'|ini=='$'|ini=='%'|ini=='@') {
			aux=valor_String.substring(1);
			if(sec=='#'|sec=='$'|sec=='%'|sec=='@') {
				result=obtieneValor(valor_String.substring(1));
				aux=Integer.toString(result);
			}
			switch(ini) {
			case '$':
				result=Integer.valueOf(aux,16);
				break;
			case '@':
				result=Integer.valueOf(aux,8);
				break;
			case '%':
				result=Integer.valueOf(aux,2);
				break;
			case '#':
				result=Integer.valueOf(aux,10);
				break;
			}
		}
		else
			result=Integer.valueOf(valor_String,10);		
		return result;
	}

	private static boolean esDirectiva(LineaASM elementAt) {
		boolean result=false;
		String inst=elementAt.getInstruccion();
		int op=-1;
		try {
			op=obtieneValor(elementAt.getOperando());
		}catch(Exception ee) {
			ee.getStackTrace();
		}
		//Directiva de constante
		if((inst.equalsIgnoreCase("DB")||inst.equalsIgnoreCase("DC.B")||inst.equalsIgnoreCase("FBC")&&(op>=0&&op<=255))) {
			elementAt.setResTabop(new ResultadoTabop(inst,"Directiva de Constante",1));
			result=true;}
		if((inst.equalsIgnoreCase("DW")||inst.equalsIgnoreCase("DC.w")||inst.equalsIgnoreCase("FDB")&&(op>=0&&op<=65535))) {
			elementAt.setResTabop(new ResultadoTabop(inst,"Directiva de Constante",2));
			result=true;}
		if(inst.equalsIgnoreCase("FCC")&&elementAt.getOperando().length()>0) {
			elementAt.setResTabop(new ResultadoTabop(inst,"Directiva de Constante",elementAt.getOperando().length()-2));
			result=true;}
		
		//Directivas de Reserva de Memoria
		if((inst.equalsIgnoreCase("DS")||inst.equalsIgnoreCase("DS.B")||inst.equalsIgnoreCase("RMB")&&(op>=0&&op<=65535))){
			elementAt.setResTabop(new ResultadoTabop(inst,"Directiva de Reserva MMRY",op));
			result=true;}
		if((inst.equalsIgnoreCase("DS.W")||inst.equalsIgnoreCase("RMW"))&&(op>=0&&op<=65535)) {
			elementAt.setResTabop(new ResultadoTabop(inst,"Directiva de Reserva MMRY",op*2));
			result=true;
		}
		
		//Directiva EQU
		if(inst.equalsIgnoreCase("EQU")&&elementAt.getEtiqueta().length()>0&&(op>=0&&op<=65535)) {
			elementAt.setResTabop(new ResultadoTabop(inst,"Directiva EQU",op));
			elementAt.setConloc(new Formatter().format("%04x", op).toString().toUpperCase());
			result=true;
		}
		//Directiva ORG
		if(inst.equalsIgnoreCase("ORG")&&(op>=0&&op<=65535)&&!huboORG) {
			elementAt.setResTabop(new ResultadoTabop(inst,"Directiva ORG",op));
			elementAt.setConloc(new Formatter().format("%04x", elContloc).toString().toUpperCase());
			elContloc=op;
			huboORG=true;
			elementAt.setProblema("");
			result=true;			
		}
		else if(inst.equalsIgnoreCase("ORG")&&(op>=0&&op<=65535)&&huboORG) {
			elementAt.setProblema("Solo puede existir un ORG!!");
			result=false;
		}
		
		if(result)
			identificaTabsim(elementAt);
		
		return result;
	}
}
