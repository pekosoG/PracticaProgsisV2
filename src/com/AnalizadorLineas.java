package com;

import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.JOptionPane;

public class AnalizadorLineas {
	
	private static String REGEX_Etiqueta="^[a-zA-Z]+([0-9]*[_]*[a-zA-Z]*)*";
	private static String REGEX_CodOP="^[a-zA-Z]+(([0-9]*[a-zA-Z]*)*[\\.]?([0-9]*[a-zA-Z]*)*)";
	private static String REGEX_Inmediato="[#][%$@#]*\\d*";
	private static String REGEX_Directo="[#%$@]*\\d*";
	private static String REGEX_Extendido=REGEX_Directo;
	private static String REGEX_xysp="(([Xx]|[Yy])|([sS][pP])|([pP][cC]))";
	private static String REGEX_Indizado="\\-{0,1}\\d*,"+REGEX_xysp+"$";
	private static String REGEX_Indizado_Indirecto="\\[\\d*,"+REGEX_xysp+"\\]";
	private static String REGEX_Indizado_PrePost="\\d*,(([-+]"+REGEX_xysp+")|("+REGEX_xysp+"[-+]))";
	private static String REGEX_Indizado_Acumulador="[AaBbDd],"+REGEX_xysp+"$";
	private static String REGEX_Indizado_Acumulador_Indirecto="\\[[AaBbDd],"+REGEX_xysp+"\\]$";
	private static String REGEX_Relativo="[^\\d*][a-zA-Z]+([0-9]*[_]*[a-zA-Z]*)*";
		
	public static Vector<LineaASM> resultado=new Vector<LineaASM>();
	public static boolean despuesEnd=false,huboEnd=false,huboORG=false;
	public static Vector<ResultadoTabop> tabop;
	public static Vector<Compara_inst> spects_ins;
	public static Vector<String> palTabsim;
	public static Map<String,String> losRegistros;
	public static Map<String,String> losPrepost;
	public static Map<String,String> losAcum;
	
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
		
		spects_ins.add(new Compara_inst("IDX",REGEX_Indizado_PrePost,1,8,"Indizado Pre-Post Decremento"));
		
		spects_ins.add(new Compara_inst("IDX",REGEX_Indizado_Acumulador,0,0,"Indizado Acumulador"));
		
		spects_ins.add(new Compara_inst("IDX1",REGEX_Indizado,-256,-17,"Indizado 9b"));
		
		spects_ins.add(new Compara_inst("IDX1",REGEX_Indizado,16,255,"Indizado 9b"));
		
		spects_ins.add(new Compara_inst("IDX2",REGEX_Indizado,256,65535,"Indizado 16b"));
		
		spects_ins.add(new Compara_inst("[IDX2]",REGEX_Indizado_Indirecto,0,65535,"Indizado 16b"));
		
		spects_ins.add(new Compara_inst("[D,IDX]",REGEX_Indizado_Acumulador_Indirecto,0,0,"Indizado Acumulador Indirecto"));

		spects_ins.add(new Compara_inst("REL",REGEX_Relativo,0,255,"Relativo 8b"));
		
		spects_ins.add(new Compara_inst("REL","("+REGEX_Etiqueta+")|("+REGEX_Relativo+")",255,65535,"Relativo 8b"));
		
		losRegistros= new HashMap<String,String>();
		losRegistros.put("X","00");
		losRegistros.put("Y","01");
		losRegistros.put("SP","10");
		losRegistros.put("PC","11");
		
		losPrepost= new HashMap<String,String>();
		losPrepost.put("+8","0111");
		losPrepost.put("+7","0110");
		losPrepost.put("+6","0101");
		losPrepost.put("+5","0100");
		losPrepost.put("+4","0011");
		losPrepost.put("+3","0010");
		losPrepost.put("+2","0001");
		losPrepost.put("+1","0000");
		losPrepost.put("-1","1111");
		losPrepost.put("-2","1110");
		losPrepost.put("-3","1101");
		losPrepost.put("-4","1100");
		losPrepost.put("-5","1011");
		losPrepost.put("-6","1010");
		losPrepost.put("-7","1001");
		losPrepost.put("-8","1000");
		
		losAcum=new HashMap<String,String>();
		losAcum.put("A","00");
		losAcum.put("B","01");
		losAcum.put("D","10");
	}
	
	public static Vector<LineaASM> procesaLineas(Vector<String> lineas){
		
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
		
		if(despuesEnd)
			System.out.println("Warning: Existen mas lineas despues del END");
		
		LineaASM aux= new LineaASM();
		aux.setInstruccion("END");
		calculaConLoc(aux);
		resultado.add(aux);
		
		//La segunda Vuleta!!!		
		for(LineaASM elementAt:resultado) {
			if(!(elementAt instanceof Comentario)&& elementAt.getProblema().length()==0) {
				if(elementAt.getOperando().length()>0&&!elementAt.getOperando().matches(REGEX_Etiqueta)) //para cambiar el OP a HEX
					try{
						elementAt.setOperando(Integer.toHexString(obtieneValor(elementAt.getOperando())));
					}catch(java.lang.NumberFormatException ee) {}
				cambiaMaq(elementAt); //cambiamos el CodMaq...
			}else
				break;
		}

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
		
		String direc="";
		try {
			direc=elementAt.getResTabop().getDireccionamiento();
		}catch(NullPointerException ee) {
			return;
		}
		
		//Para el INH no hace falta hacer este desmadreeee!!!....
		if(direc.equalsIgnoreCase("INH")) {
			String cod=elementAt.getResTabop().getCodmaquina();
			elementAt.setCodMaq(cod);
		}
		
		if(direc.equalsIgnoreCase("DIR")) {
			String auxMaq=elementAt.getResTabop().getCodmaquina();
			String auxOp=elementAt.getOperando();
			auxMaq=auxMaq.substring(0,auxMaq.length()-3);
			auxMaq+=auxOp;
			elementAt.setCodMaq(auxMaq);
		}
		
		if(direc.equalsIgnoreCase("EXT")) {
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
		
		if(direc.equalsIgnoreCase("IMM")) {
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
		
		if(direc.equalsIgnoreCase("IDX")||direc.equalsIgnoreCase("IDX1")||direc.equalsIgnoreCase("IDX2")) {
			if(elementAt.getOperando().matches(REGEX_Indizado)) {
				String elNumOP=elementAt.getOperando().replaceAll(",[^\\-{0,1}\\d*#$%@]*"," ").trim();
				int valOP=0;
				if(elNumOP.length()>0)
					valOP=obtieneValor(elNumOP);
				if(valOP>=-16 && valOP<=15) //IDX de 5b
					elementAt.setCodMaq(paraIDX5b(elementAt,valOP));
				if((valOP>=-256&&valOP<=-17)||(valOP>=16 && valOP<=255))
					elementAt.setCodMaq(paraIDX9b(elementAt,valOP,1));
				if(valOP>=256 && valOP<=65535)
					elementAt.setCodMaq(paraIDX9b(elementAt,valOP,2));
			}
			else{
				if(elementAt.getOperando().matches(REGEX_Indizado_PrePost)) {//Para los Pre-Post
					elementAt.setCodMaq(checaPrePost(elementAt));					
				}
				if(elementAt.getOperando().matches(REGEX_Indizado_Acumulador)) {
					elementAt.setCodMaq(checaIdxAcum(elementAt));
				}
			}
		}
		//Falta hacer la rutina para que sean cadenas HEX bonitas...
		
		if(direc.equalsIgnoreCase("[IDX2]")){
			elementAt.setCodMaq(paraIDX16(elementAt,1));
		}
		if(direc.equalsIgnoreCase("[D,IDX]")){
			elementAt.setCodMaq(paraIDX16(elementAt,2));
		}
		
		if(direc.equalsIgnoreCase("REL")) {
			elementAt.setCodMaq(paraREL(elementAt));
		}
	}
	
	private static String paraREL(LineaASM elementAt) {
		boolean encontrado=false;
		String result="",nextCont="";
		String tempOp=elementAt.getOperando();
		
		for(String ee:palTabsim) {
			String a[]=ee.split("\\s");
			ee=a[0];
			if(ee.equalsIgnoreCase(tempOp)) {
				tempOp=a[1];
				encontrado=true;
				break;
			}
		}
		if(encontrado) {
			int index=resultado.indexOf(elementAt);
			if(index<resultado.size()) {
				nextCont=resultado.get(index+1).getConloc();
				int diferencia=Integer.valueOf(tempOp, 16)-Integer.valueOf(nextCont, 16);
				int rangos[]= {-128,127};
				int mochale=2;
				
				if(elementAt.getInstruccion().charAt(0)=='L' || elementAt.getInstruccion().charAt(0)=='l') {
					rangos[0]=-32768;
					rangos[1]=32767;
					mochale=5;
				}
				
				if(diferencia>=rangos[0]&&diferencia<=rangos[1]) {
					result=elementAt.getResTabop().getCodmaquina();
					//System.out.println(result+"----");
					result=result.substring(0, result.length()-mochale);
					
					nextCont=Integer.toHexString(diferencia).toUpperCase();
					if(mochale==2)
						nextCont=String.format("%2s", nextCont).replace(' ','0');
					
					if(mochale==5)
						nextCont=String.format("%4s", nextCont).replace(' ','0');

					result+=nextCont;
				}				
				
			}
		}
		
		return result;
	}
	
	private static String paraIDX16(LineaASM elementAt,int ext) {
		String result="111",elOP=elementAt.getOperando(),aux="";
		String elNumOP="";
		if(ext==1)
			elNumOP=elOP.substring(1,elOP.indexOf(","));
		String elValor=elOP.substring(elOP.indexOf(",")+1, elOP.length()-1);
		
		elValor=losRegistros.get(elValor);
		
		if(ext==1)
			result+=elValor+"011";
		else
			result+=elValor+"111";
			
		//La cadena Binaria, la convertimos a su valor Entero, para despues convertirla a Hex
		result=Integer.toHexString(Integer.valueOf(result, 2)).toUpperCase();
		result=String.format("%2s", result).replace(' ','0');//Le damos el formato de FFFF
		
		aux=elementAt.getResTabop().getCodmaquina();
		
		//Quitamos los bytes necesarios y los sustituimos con el calculado
		if(ext==1)
			aux=aux.substring(0, aux.length()-9);
		else
			aux=aux.substring(0, aux.length()-3);
		aux+=result;
		
		if(ext==1) {
			String pre=Integer.toHexString(Integer.valueOf(elNumOP));
			pre=String.format("%4s", pre).replace(' ','0').toUpperCase();
			aux+=pre;
		}
		
		return aux;
	}
	
	private static String checaIdxAcum(LineaASM elementAt) {
		String result="111",elOp=elementAt.getOperando();
		String aux="";
		
		String acum=elOp.substring(0,1);
		String elSPPC=elOp.substring(elOp.indexOf(",")+1,elOp.length()).toUpperCase();
		
		String value=losRegistros.get(elSPPC);
		acum=losAcum.get(acum);
		
		result+=value+"1"+acum;

		//La cadena Binaria, la convertimos a su valor Entero, para despues convertirla a Hex
		result=Integer.toHexString(Integer.valueOf(result, 2)).toUpperCase();
		result=String.format("%2s", result).replace(' ','0');//Le damos el formato de FFFF

		aux=elementAt.getResTabop().getCodmaquina();

		//Quitamos los bytes necesarios y los sustituimos con el calculado
		aux=aux.substring(0, aux.length()-3);
		aux+=result;
		return aux;
	}
	
	private static String checaPrePost(LineaASM elementAt) {
		String result="",elOperando=elementAt.getOperando();
		String aux;
		
		String numero=elOperando.substring(0,1);
		String elSPPC=elOperando.substring(elOperando.indexOf(",")+1,elOperando.length());
		int posSign=elSPPC.indexOf("+");
		if(posSign==-1)
			posSign=elSPPC.indexOf("-");
		char signo=elSPPC.charAt(posSign);
		elSPPC=elSPPC.replaceAll("[+-]"," ").trim();
		
		System.out.println(elOperando+" * "+elSPPC+" * "+posSign+" * "+signo+" * "+numero);
		
		String value=losRegistros.get(elSPPC);
		
		result+=value+"1";
		if(posSign==0)
			result+="0";
		else
			result+="1";
		aux=signo+numero;
		
		result+=losPrepost.get(aux);
		
		//La cadena Binaria, la convertimos a su valor Entero, para despues convertirla a Hex
		result=Integer.toHexString(Integer.valueOf(result, 2)).toUpperCase();
		result=String.format("%2s", result).replace(' ','0');//Le damos el formato de FFFF
		
		aux=elementAt.getResTabop().getCodmaquina();
		
		//Quitamos los bytes necesarios y los sustituimos con el calculado
		aux=aux.substring(0, aux.length()-3);
		aux+=result;
		return aux;

	}
	
	private static String paraIDX5b(LineaASM elementAt,int valOP) {
		//Operandos están en formato NUM*,
		String result="",aux="";
		
		//ESta linea obtiene el valor de X,Y,SP,PC del Operando
		String elNumOP=elementAt.getOperando().substring(elementAt.getOperando().indexOf(',')+1).toUpperCase().trim();
		String value=losRegistros.get(elNumOP); //Aqui obtenemos el valor binario de eso
		
		result+=value+"0"; //aqui tenemos rr0
		
		//Aqui obtenemos la cadena binaria del numero del operando
		aux=Integer.toBinaryString(valOP); 
		aux=String.format("%4s", aux).replace(' ', '0');//y le damos el formato de nnnnn
		
		if(aux.length()>4)
			aux=aux.substring(aux.length()-4, aux.length());
		
		result+=aux; //Aqui ya tenemos rr0nnnnn
		
		//La cadena Binaria, la convertimos a su valor Entero, para despues convertirla a Hex
		result=Integer.toHexString(Integer.valueOf(result, 2)).toUpperCase();
		result=String.format("%2s", result).replace(' ','0');//Le damos el formato de FFFF
		
		
		aux=elementAt.getResTabop().getCodmaquina();
		
		//Quitamos los bytes necesarios y los sustituimos con el calculado
		aux=aux.substring(0, aux.length()-3);
		aux+=result;
		return aux;
	}
	
	private static String paraIDX9b(LineaASM elementAt,int valOP,int ext) {
		String result="111",aux="";
		
		//ESta linea obtiene el valor de X,Y,SP,PC del Operando
		String elNumOP=elementAt.getOperando().substring(elementAt.getOperando().indexOf(',')+1).toUpperCase().trim();
		String value=losRegistros.get(elNumOP); //Aqui obtenemos el valor binario de eso
		
		if(ext==1)
			result+=value+"000"; //Aqui ya tenemos 111nn0
		else
			result+=value+"010";
		
		//La cadena Binaria, la convertimos a su valor Entero, para despues convertirla a Hex
		result=Integer.toHexString(Integer.valueOf(result, 2)).toUpperCase();
		result=String.format("%2s", result).replace(' ','0');//Le damos el formato de FF
		
		aux=elementAt.getResTabop().getCodmaquina();
		
		//Quitamos los bytes necesarios y los sustituimos con el calculado
		if(ext==1)
			aux=aux.substring(0, aux.length()-6);
		else
			aux=aux.substring(0, aux.length()-9);
		aux+=result+Integer.toHexString(valOP);
		result=aux.toUpperCase();
		
		return result;
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
			if(!elementAt.getInstruccion().equalsIgnoreCase("END"))
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
								try {
									String aux[]=valor_String.split(",");
									int valorElement;
									if(aux[0].length()>0)
										valorElement=obtieneValor(aux[0]);
									else valorElement=0;
									if(valorElement>=comp_aux.min&&valorElement<=comp_aux.max) {
										if(elementAt.getOperando().matches(comp_aux.REGEX)) {
											result=true;
											elementAt.setResult(comp_aux.Descrip);
											elementAt.setResTabop(resAux);
											break;
										}
									}else { elementAt.setProblema("");elementAt.setProblema("Error! Operando Invalido 2");}
								}catch(NumberFormatException NFE2) {
									if(valor_String.matches(comp_aux.REGEX)) {
										result=true;
										elementAt.setResult(comp_aux.Descrip);
										elementAt.setResTabop(resAux);
										break;
									}else { elementAt.setProblema("");elementAt.setProblema("Error! Operando Invalido 3");}
								}
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
