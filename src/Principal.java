import java.util.Vector;

import com.AnalizadorLineas;
import com.LineaASM;
import com.ManejaArchivo;

public class Principal {

	public static Vector<LineaASM> lineasASM;
	public static Vector<String> resTabop;
	
	static {
		lineasASM=new Vector<LineaASM>();
		resTabop= new Vector<String>();
	}
	
	public static void main(String[] args) {
		lineasASM=AnalizadorLineas.procesaLineas(ManejaArchivo.leeASM("prueba.txt"));
		
		for(LineaASM tmp:lineasASM) 
			System.out.println(tmp);
		ManejaArchivo.escribeLTS(lineasASM,"prueba.lst");
	}

}
